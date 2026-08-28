package com.embabel.dif.dif;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.canvas.CanvasFolder;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.ProposedChange;
import com.embabel.dif.domain.SemanticSnapshot;
import com.embabel.dif.domain.TestExecution;
import com.embabel.dif.scenario.RefreshTokenScenario;
import com.embabel.dif.verifier.RuleBasedSemanticVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 1 — the five success checks are named tests, not only prose.
 */
class FoldContractTest {

    private final CanvasFolder folder = FoldWiring.canvasFolder();

    @Test
    void sameAcceptedCanvasFoldsToTheSameModel() throws Exception {
        var markdown = canvas("FEAT-001-order-status-api.md");
        assertThat(folder.fold(markdown)).isEqualTo(folder.fold(markdown));
    }

    @Test
    void reviewFailsWithoutAskingWhetherTheChangeLooksCorrect() throws Exception {
        var mapper = new ObjectMapper();
        var before = mapper.readValue(
                Path.of("examples/snapshots/login-before.json").toFile(),
                SemanticSnapshot.class
        );
        var broken = mapper.readValue(
                Path.of("examples/snapshots/login-auth-broken.json").toFile(),
                SemanticSnapshot.class
        );
        var model = new RuleBasedIntentFolder(new ConflictDetector())
                .fold(RefreshTokenScenario.candidateIntent());
        var verifier = new RuleBasedSemanticVerifier();
        var diff = verifier.diff(before, broken);
        var verification = verifier.verify(
                model,
                new ProposedChange(List.of()),
                new TestExecution(true, List.of()),
                diff
        );

        assertThat(diff.preserves(RuleBasedSemanticVerifier.REQUIRED_UNCHANGED)).isFalse();
        assertThat(verification.passed()).isFalse();
        assertThat(verification.results())
                .anyMatch(result -> result.failureReason().orElse("").contains("were not preserved"));
    }

    @Test
    void syntaxVarianceDoesNotFlipRequiredInvariants() throws Exception {
        var dto = folder.fold(canvas("FEAT-070-dto-rename-a.md"));
        var renamed = folder.fold(canvas("FEAT-070-dto-rename-b.md"));

        assertThat(invariantDescriptions(dto)).isEqualTo(invariantDescriptions(renamed));
        assertThat(invariantDescriptions(dto))
                .anyMatch(text -> text.contains("auth") || text.contains("unrelated"));
    }

    @Test
    void openOperationIsAMissingObligation() throws Exception {
        var model = folder.fold(canvas("FEAT-001-order-status-api.md"));

        assertThat(model.missingObligations())
                .anyMatch(obligation -> obligation.obligation().contains("T03"));
        assertThat(model.missingObligations())
                .noneMatch(obligation -> obligation.obligation().contains("T01"));
    }

    @Test
    void requirementVersusNonGoalBlocksReadyForCoding() throws Exception {
        var model = folder.fold(canvas("FEAT-099-pagination-conflict.md"));

        assertThat(model.hasBlockingConflicts()).isTrue();
        assertThat(model.intents())
                .anyMatch(intent -> intent.type() == IntentType.REQUIREMENT
                        && intent.statement().toLowerCase().contains("paginat"));
        assertThat(model.intents())
                .anyMatch(intent -> intent.type() == IntentType.CONSTRAINT
                        && intent.statement().toLowerCase().contains("paginat"));
    }

    private static List<String> invariantDescriptions(com.embabel.dif.domain.SemanticModel model) {
        return model.invariants().stream().map(invariant -> invariant.description()).toList();
    }

    private static String canvas(String name) throws Exception {
        return Files.readString(Path.of("examples/canvases", name), StandardCharsets.UTF_8);
    }
}
