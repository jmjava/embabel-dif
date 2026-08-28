package com.embabel.dif.cli;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.dif.AlloyModelEmitter;
import com.embabel.dif.domain.ProposedChange;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.SemanticSnapshot;
import com.embabel.dif.domain.TestExecution;
import com.embabel.dif.memory.GuideLedger;
import com.embabel.dif.memory.SemanticModelRenderer;
import com.embabel.dif.scenario.RefreshTokenScenario;
import com.embabel.dif.verifier.RuleBasedSemanticVerifier;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Standalone fold and gate entry point. Does not start Embabel or Spring.
 *
 * <pre>
 *   ./scripts/dif-fold.sh --canvas examples/canvases/FEAT-001-order-status-api.md
 *   ./scripts/dif-fold.sh architect --projection .dif/projections/FEAT-099-pagination-conflict.json
 * </pre>
 */
public final class DifCli {

    private DifCli() {
    }

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    static int run(String[] args) throws Exception {
        if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("""
                    Usage:
                      fold --canvas <file> [--out <dir>] [--guide] [--alloy] [--quiet]
                      architect --canvas <file> [--out <dir>] [--quiet]
                      architect --projection <file> [--quiet]
                      review --before <snapshot.json> --after <snapshot.json> [--canvas <file>] [--quiet]
                      plan --canvas <file> [--out <dir>]
                      plan --projection <file> [--out <dir>]
                      guide --canvas <file> [--out <dir>]
                    
                    fold writes a deterministic SemanticModel projection and .gate.json.
                    architect / review fail closed from the projection or IntentDiff.
                    --quiet prints one line: dif=ready|blocked (orch attach).
                    plan --projection reads a folded model; it does not re-parse markdown.
                    Exit 1 if blocking conflicts exist or review invariants fail.
                    """);
            return args.length == 0 ? 2 : 0;
        }
        try {
            return switch (args[0]) {
                case "fold" -> fold(args);
                case "architect" -> architect(args);
                case "review" -> review(args);
                case "plan" -> plan(args);
                case "guide" -> guide(args);
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    yield 2;
                }
            };
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return 2;
        }
    }

    private static int fold(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.canvasPath == null) {
            System.err.println("--canvas is required");
            return 2;
        }
        var written = writeFold(flags);
        if (flags.quiet) {
            System.out.println(written.gate().oneLine());
        } else {
            System.out.print(SemanticModelRenderer.render(written.model()));
            System.out.println("wrote " + written.jsonPath());
            System.out.println("wrote " + written.gatePath());
        }
        return written.exitCode();
    }

    private static int architect(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.projectionPath != null) {
            return architectFromProjection(flags);
        }
        if (flags.canvasPath == null) {
            System.err.println("--canvas or --projection is required");
            return 2;
        }
        var written = writeFold(flags);
        if (flags.quiet) {
            System.out.println(written.gate().oneLine());
        } else {
            printArchitect(written.workId(), written.blocking(), SemanticModelRenderer.render(written.model()));
        }
        return written.exitCode();
    }

    private static WrittenFold writeFold(Flags flags) throws Exception {
        var markdown = Files.readString(flags.canvasPath, StandardCharsets.UTF_8);
        var folder = FoldWiring.canvasFolder();
        var canvas = folder.parse(markdown);
        var model = folder.fold(canvas);
        var projection = new FoldProjection(
                canvas.workId(),
                flags.canvasPath.toString(),
                !model.hasBlockingConflicts(),
                model
        );

        Files.createDirectories(flags.outDir);
        var jsonPath = flags.outDir.resolve(canvas.workId() + ".json");
        var gatePath = flags.outDir.resolve(canvas.workId() + ".gate.json");
        var textPath = flags.outDir.resolve(canvas.workId() + ".txt");
        mapper().writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), projection);
        mapper().writerWithDefaultPrettyPrinter().writeValue(gatePath.toFile(), GateReport.from(canvas.workId(), model));
        Files.writeString(textPath, SemanticModelRenderer.render(model), StandardCharsets.UTF_8);
        if (flags.alloy) {
            Files.writeString(
                    flags.outDir.resolve(canvas.workId() + ".als"),
                    AlloyModelEmitter.emit(canvas.workId(), model),
                    StandardCharsets.UTF_8
            );
        }
        if (flags.guide) {
            Files.writeString(
                    flags.outDir.resolve(canvas.workId() + ".guide.jsonl"),
                    GuideLedger.jsonl(canvas.workId(), model),
                    StandardCharsets.UTF_8
            );
        }
        return new WrittenFold(canvas.workId(), model, jsonPath, gatePath);
    }

    private static int architectFromProjection(Flags flags) throws Exception {
        var root = mapper().readTree(flags.projectionPath.toFile());
        var workId = root.path("workId").asText("UNKNOWN");
        var ready = root.path("readyForImplementation").asBoolean(false);
        var conflicts = root.path("model").path("conflicts");
        var conflictCount = conflicts.isArray() ? conflicts.size() : 0;
        if (flags.quiet) {
            System.out.println(GateReport.oneLine(workId, ready, conflictCount));
            return ready ? 0 : 1;
        }
        var report = new StringBuilder();
        report.append("architect workId=").append(workId).append('\n');
        report.append("readyForImplementation=").append(ready).append('\n');
        report.append("source=projection\n");
        if (!conflicts.isArray() || conflicts.isEmpty()) {
            report.append("conflicts:\n  - none\n");
        } else {
            report.append("conflicts:\n");
            for (JsonNode conflict : conflicts) {
                report.append("  - ")
                        .append(conflict.path("left").path("statement").asText())
                        .append(" vs ")
                        .append(conflict.path("right").path("statement").asText())
                        .append(" (")
                        .append(conflict.path("explanation").asText())
                        .append(")\n");
            }
        }
        System.out.print(report);
        return ready ? 0 : 1;
    }

    private record WrittenFold(String workId, SemanticModel model, Path jsonPath, Path gatePath) {
        GateReport gate() {
            return GateReport.from(workId, model);
        }

        boolean blocking() {
            return model.hasBlockingConflicts();
        }

        int exitCode() {
            return blocking() ? 1 : 0;
        }
    }

    private static void printArchitect(String workId, boolean blocking, String rendered) {
        System.out.println("architect workId=" + workId);
        System.out.println("readyForImplementation=" + !blocking);
        System.out.println("source=canvas");
        System.out.print(rendered);
    }

    private static int review(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.beforePath == null || flags.afterPath == null) {
            System.err.println("review requires --before and --after snapshots");
            return 2;
        }
        var before = mapper().readValue(flags.beforePath.toFile(), SemanticSnapshot.class);
        var after = mapper().readValue(flags.afterPath.toFile(), SemanticSnapshot.class);
        var model = flags.canvasPath != null
                ? FoldWiring.canvasFolder().fold(Files.readString(flags.canvasPath, StandardCharsets.UTF_8))
                : new com.embabel.dif.dif.RuleBasedIntentFolder(new com.embabel.dif.dif.ConflictDetector())
                        .fold(RefreshTokenScenario.candidateIntent());

        var verifier = new RuleBasedSemanticVerifier();
        var diff = verifier.diff(before, after);
        var canvasPaths = flags.canvasPath != null
                ? com.embabel.dif.verifier.SafeguardPaths.fromCanvasSafeguards(model)
                : Set.<String>of();
        var verification = canvasPaths.isEmpty()
                ? verifier.verify(model, new ProposedChange(List.of()), new TestExecution(true, List.of()), diff)
                : verifier.verify(model, new ProposedChange(List.of()), new TestExecution(true, List.of()), diff, canvasPaths);
        var workId = flags.canvasPath != null
                ? FoldWiring.canvasFolder().parse(Files.readString(flags.canvasPath, StandardCharsets.UTF_8)).workId()
                : "review";
        if (flags.quiet) {
            System.out.println(verification.passed()
                    ? "dif=ready workId=" + workId + " passed=true"
                    : "dif=blocked workId=" + workId + " passed=false");
        } else {
            System.out.println("review passed=" + verification.passed());
            System.out.println(diff.render());
            verification.results().forEach(result ->
                    System.out.println(result.status() + " " + result.invariantId()
                            + result.failureReason().map(reason -> ": " + reason).orElse("")));
        }
        return verification.passed() ? 0 : 1;
    }

    private static int plan(String[] args) throws Exception {
        var flags = Flags.parse(args);
        String workId;
        com.embabel.dif.domain.VerificationPlan verificationPlan;
        if (flags.projectionPath != null) {
            var projection = mapper().readValue(flags.projectionPath.toFile(), FoldProjection.class);
            workId = projection.workId();
            verificationPlan = FoldWiring.planner().plan(projection.model());
        } else if (flags.canvasPath != null) {
            var folder = FoldWiring.canvasFolder();
            var canvas = folder.parse(Files.readString(flags.canvasPath, StandardCharsets.UTF_8));
            workId = canvas.workId();
            verificationPlan = FoldWiring.planner().plan(folder.fold(canvas));
        } else {
            System.err.println("--canvas or --projection is required");
            return 2;
        }
        Files.createDirectories(flags.outDir);
        var jsonPath = flags.outDir.resolve(workId + ".plan.json");
        var payload = new LinkedHashMap<String, Object>();
        payload.put("workId", workId);
        payload.put("readyForImplementation", verificationPlan.readyForImplementation());
        payload.put("ruleCount", verificationPlan.rules().size());
        payload.put("missingObligations", verificationPlan.missingObligations());
        payload.put("rules", verificationPlan.rules());
        mapper().writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), payload);
        if (flags.quiet) {
            System.out.println(verificationPlan.readyForImplementation()
                    ? "dif=ready workId=" + workId + " readyForImplementation=true"
                    : "dif=blocked workId=" + workId + " readyForImplementation=false");
        } else {
            System.out.println("plan workId=" + workId);
            System.out.println("readyForImplementation=" + verificationPlan.readyForImplementation());
            System.out.println("rules=" + verificationPlan.rules().size());
            System.out.print(SemanticModelRenderer.render(verificationPlan.model()));
            System.out.println("wrote " + jsonPath);
        }
        return verificationPlan.readyForImplementation() ? 0 : 1;
    }

    private static int guide(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.canvasPath == null) {
            System.err.println("--canvas is required");
            return 2;
        }
        var folder = FoldWiring.canvasFolder();
        var canvas = folder.parse(Files.readString(flags.canvasPath, StandardCharsets.UTF_8));
        var model = folder.fold(canvas);
        Files.createDirectories(flags.outDir);
        var jsonlPath = flags.outDir.resolve(canvas.workId() + ".guide.jsonl");
        Files.writeString(jsonlPath, GuideLedger.jsonl(canvas.workId(), model), StandardCharsets.UTF_8);
        System.out.print(Files.readString(jsonlPath, StandardCharsets.UTF_8));
        System.out.println("wrote " + jsonlPath);
        return 0;
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final class Flags {
        private Path canvasPath;
        private Path outDir = Path.of(".dif/projections");
        private Path projectionPath;
        private Path beforePath;
        private Path afterPath;
        private boolean guide;
        private boolean alloy;
        private boolean quiet;

        private static Flags parse(String[] args) {
            var flags = new Flags();
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--canvas" -> flags.canvasPath = Path.of(requireValue(args, ++i, "--canvas"));
                    case "--out" -> flags.outDir = Path.of(requireValue(args, ++i, "--out"));
                    case "--projection" -> flags.projectionPath = Path.of(requireValue(args, ++i, "--projection"));
                    case "--before" -> flags.beforePath = Path.of(requireValue(args, ++i, "--before"));
                    case "--after" -> flags.afterPath = Path.of(requireValue(args, ++i, "--after"));
                    case "--guide" -> flags.guide = true;
                    case "--alloy" -> flags.alloy = true;
                    case "--quiet" -> flags.quiet = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            return flags;
        }
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[index];
    }
}
