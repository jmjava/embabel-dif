package com.embabel.dif.ingest;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.dif.FoldWiring;
import com.embabel.dif.agent.DifEmbabelAgent;
import com.embabel.dif.agent.FixtureIntentInterpreter;
import com.embabel.dif.agent.RepositoryAnalyzer;
import com.embabel.dif.canvas.CanvasIntentMapper;
import com.embabel.dif.canvas.ReasonsCanvasParser;
import com.embabel.dif.cli.GateReport;
import com.embabel.dif.dif.ConflictDetector;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.dif.RuleBasedIntentFolder;
import com.embabel.dif.dif.VerificationPlanner;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.memory.GuideLedger;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How data gets into each model. Each test is one hop. Nothing here
 * starts Embabel, orch, or Guide — those consume what these hops write.
 */
class DataModelIngestTest {

    @Test
    void hop1_parserTakesMappedBulletsAndDropsUserGoalProse() throws Exception {
        var canvas = new ReasonsCanvasParser().parse(feat001());

        assertThat(canvas.workId()).isEqualTo("FEAT-001-order-status-api");
        assertThat(canvas.readiness()).isEqualTo("Ready For Coding");
        assertThat(canvas.acceptanceCriteria()).containsExactly(
                "`GET /api/orders?email=` returns matching orders",
                "Invalid email format returns 400",
                "Empty result returns 200 with empty list",
                "Tests cover service and controller behavior"
        );
        assertThat(canvas.nonGoals()).containsExactly("Pagination", "Auth changes", "Schema migration");
        assertThat(canvas.safeguards()).contains("Do not change auth behavior");
        assertThat(canvas.norms()).contains("Constructor injection");
        assertThat(canvas.entities()).containsExactly("Order");
        assertThat(canvas.filesLikelyAffected()).contains("src/main/java/.../OrderController.java");
        assertThat(canvas.operations())
                .anyMatch(op -> op.id().equals("T03") && !op.complete())
                .anyMatch(op -> op.id().equals("T01") && op.complete());
        assertThat(canvas.acceptanceCriteria().stream().collect(java.util.stream.Collectors.joining(" ")))
                .doesNotContain("Search orders by customer email")
                .doesNotContain("Reduce support response time");
    }

    @Test
    void hop2_mapperWritesTypesFromHeadingsNotFromWording() throws Exception {
        var canvas = new ReasonsCanvasParser().parse(feat001());
        var candidate = new CanvasIntentMapper().toCandidate(canvas);

        assertThat(candidate.intents())
                .filteredOn(intent -> intent.type() == IntentType.REQUIREMENT)
                .extracting(intent -> intent.statement())
                .contains("`GET /api/orders?email=` returns matching orders");
        assertThat(candidate.intents())
                .filteredOn(intent -> intent.type() == IntentType.CONSTRAINT)
                .extracting(intent -> intent.statement())
                .contains("Non-goal: Pagination");
        assertThat(candidate.intents())
                .filteredOn(intent -> intent.type() == IntentType.PRESERVATION)
                .extracting(intent -> intent.statement())
                .contains("Do not change auth behavior", "Constructor injection");
        assertThat(candidate.intents())
                .allMatch(intent -> intent.provenance().source().startsWith("FEAT-001-order-status-api:"));
        assertThat(candidate.evidence())
                .anyMatch(evidence -> evidence.type() == EvidenceType.DOCUMENTATION
                        && evidence.observation().contains("customerEmail"))
                .anyMatch(evidence -> evidence.type() == EvidenceType.SOURCE_CODE
                        && evidence.observation().contains("Domain entity Order"))
                .anyMatch(evidence -> evidence.source().contains("OrderController.java"));
        assertThat(candidate.intents())
                .noneMatch(intent -> intent.statement().contains("T03"));
    }

    @Test
    void hop3_folderFreezesCandidateAndCanvasFolderAddsOpenOperations() throws Exception {
        var markdown = feat001();
        var canvas = FoldWiring.canvasFolder().parse(markdown);
        var candidate = new CanvasIntentMapper().toCandidate(canvas);
        var foldedOnly = new RuleBasedIntentFolder(new ConflictDetector()).fold(candidate);
        var withAbsence = FoldWiring.canvasFolder().fold(canvas);

        assertThat(foldedOnly.intents()).hasSameSizeAs(candidate.intents());
        assertThat(foldedOnly.missingObligations()).isEmpty();
        assertThat(foldedOnly.hasBlockingConflicts()).isFalse();
        assertThat(foldedOnly.invariants())
                .noneMatch(invariant -> invariant.description().startsWith("Preserve: Non-goal:"));
        assertThat(withAbsence.intents()).isEqualTo(foldedOnly.intents());
        assertThat(withAbsence.missingObligations())
                .anyMatch(obligation -> obligation.obligation().contains("T03")
                        && obligation.derivedFromIntent().equals("FEAT-001-order-status-api"))
                .noneMatch(obligation -> obligation.obligation().contains("T01"));
    }

    @Test
    void hop3_requirementVersusNonGoalWritesABlockingConflict() throws Exception {
        var model = FoldWiring.canvasFolder().fold(feat099());

        assertThat(model.hasBlockingConflicts()).isTrue();
        assertThat(model.conflicts())
                .anyMatch(conflict -> conflict.explanation().contains("Requirement conflicts with a canvas non-goal"));
        assertThat(model.intents())
                .anyMatch(intent -> intent.type() == IntentType.REQUIREMENT
                        && intent.statement().toLowerCase().contains("paginat"))
                .anyMatch(intent -> intent.type() == IntentType.CONSTRAINT
                        && intent.statement().toLowerCase().contains("paginat"));
    }

    @Test
    void hop4_gateAndPlanReadTheFreezeTheyDoNotClassify() throws Exception {
        var model = FoldWiring.canvasFolder().fold(feat001());
        var gate = GateReport.from("FEAT-001-order-status-api", model);
        var plan = FoldWiring.planner().plan(model);

        assertThat(gate.readyForImplementation()).isTrue();
        assertThat(gate.oneLine()).isEqualTo(
                "dif=ready workId=FEAT-001-order-status-api readyForImplementation=true"
        );
        assertThat(gate.missingObligations()).anyMatch(text -> text.contains("T03"));
        assertThat(plan.model().intents()).isEqualTo(model.intents());
        assertThat(plan.rules()).hasSameSizeAs(model.invariants());
        assertThat(plan.readyForImplementation()).isTrue();
        assertThat(plan.missingObligations()).isEqualTo(model.missingObligations());
    }

    @Test
    void hop5_guideLedgerQuotesInvariantsAndHolesItDoesNotFold() throws Exception {
        var model = FoldWiring.canvasFolder().fold(feat001());
        var jsonl = GuideLedger.jsonl("FEAT-001-order-status-api", model);

        assertThat(jsonl).contains("\"kind\":\"Decision\"");
        assertThat(jsonl).contains("\"kind\":\"Pitfall\"");
        assertThat(jsonl).contains("\"workId\":\"FEAT-001-order-status-api\"");
        assertThat(jsonl).contains("Missing: T03");
        assertThat(jsonl).doesNotContain("Search orders by customer email");
        assertThat(model.invariants()).isNotEmpty();
        for (var invariant : model.invariants()) {
            assertThat(jsonl).contains(invariant.description());
        }
    }

    @Test
    void embabelIngestsRequestTextNotTheCanvas() {
        var agent = new DifEmbabelAgent(
                List.of(new FixtureIntentInterpreter()),
                new RuleBasedIntentFolder(new ConflictDetector()),
                new RepositoryAnalyzer(),
                new VerificationPlanner(new ObligationDeriver())
        );

        var request = agent.captureRequest(new UserInput(RefreshTokenScenario.REQUEST_TEXT));
        var candidate = agent.interpretIntent(request, null);
        var model = agent.foldIntent(candidate);
        var analysis = agent.analyzeRepository(request, model);
        var plan = agent.planVerification(model, analysis);

        assertThat(request.text()).isEqualTo(RefreshTokenScenario.REQUEST_TEXT);
        assertThat(candidate.intents()).extracting(intent -> intent.id())
                .containsExactly("INT-001", "INT-002", "INT-003", "INT-004", "INT-005");
        assertThat(candidate.intents())
                .noneMatch(intent -> intent.statement().contains("GET /api/orders"));
        assertThat(model.invariants())
                .anyMatch(invariant -> invariant.id().equals("INV-001"))
                .anyMatch(invariant -> invariant.id().equals("INV-GOOGLE"));
        assertThat(analysis.evidence())
                .anyMatch(evidence -> evidence.observation().contains("Google OAuth"));
        assertThat(plan.missingObligations())
                .anyMatch(obligation -> obligation.obligation().equals("rotation integration test"));
        assertThat(plan.rules()).isNotEmpty();
    }

    @Test
    void fixtureInterpreterDoesNotWriteASemanticModel() {
        var interpreter = new FixtureIntentInterpreter();
        var candidate = interpreter.interpret(RefreshTokenScenario.request(), null);

        assertThat(candidate.intents()).hasSize(5);
        assertThat(candidate.evidence()).hasSize(9);
        assertThat(candidate.getClass().getSimpleName()).isEqualTo("CandidateIntent");
    }

    private static String feat001() throws Exception {
        return Files.readString(
                Path.of("examples/canvases/FEAT-001-order-status-api.md"),
                StandardCharsets.UTF_8
        );
    }

    private static String feat099() throws Exception {
        return Files.readString(
                Path.of("examples/canvases/FEAT-099-pagination-conflict.md"),
                StandardCharsets.UTF_8
        );
    }
}
