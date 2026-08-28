package com.embabel.dif.cli;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.dif.AlloyModelEmitter;
import com.embabel.dif.domain.ProposedChange;
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
                      fold --canvas <file> [--out <dir>] [--guide] [--alloy]
                      architect --canvas <file> [--out <dir>]
                      architect --projection <file>
                      review --before <snapshot.json> --after <snapshot.json> [--canvas <file>]
                      plan --canvas <file> [--out <dir>]
                      guide --canvas <file> [--out <dir>]
                    
                    fold writes a deterministic SemanticModel projection and .gate.json.
                    architect / review fail closed from the projection or IntentDiff.
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

        System.out.print(SemanticModelRenderer.render(model));
        System.out.println("wrote " + jsonPath);
        System.out.println("wrote " + gatePath);
        return model.hasBlockingConflicts() ? 1 : 0;
    }

    private static int architect(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.projectionPath != null) {
            return architectFromProjection(flags.projectionPath);
        }
        if (flags.canvasPath == null) {
            System.err.println("--canvas or --projection is required");
            return 2;
        }
        var foldCode = fold(args);
        var markdown = Files.readString(flags.canvasPath, StandardCharsets.UTF_8);
        var folder = FoldWiring.canvasFolder();
        var canvas = folder.parse(markdown);
        var model = folder.fold(canvas);
        printArchitect(canvas.workId(), model.hasBlockingConflicts(), SemanticModelRenderer.render(model));
        return foldCode;
    }

    private static int architectFromProjection(Path projectionPath) throws Exception {
        var root = mapper().readTree(projectionPath.toFile());
        var workId = root.path("workId").asText("UNKNOWN");
        var ready = root.path("readyForImplementation").asBoolean(false);
        var report = new StringBuilder();
        report.append("architect workId=").append(workId).append('\n');
        report.append("readyForImplementation=").append(ready).append('\n');
        report.append("source=projection\n");
        var conflicts = root.path("model").path("conflicts");
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
        var verification = verifier.verify(
                model,
                new ProposedChange(List.of()),
                new TestExecution(true, List.of()),
                diff
        );
        System.out.println("review passed=" + verification.passed());
        System.out.println(diff.render());
        verification.results().forEach(result ->
                System.out.println(result.status() + " " + result.invariantId()
                        + result.failureReason().map(reason -> ": " + reason).orElse("")));
        return verification.passed() ? 0 : 1;
    }

    private static int plan(String[] args) throws Exception {
        var flags = Flags.parse(args);
        if (flags.canvasPath == null) {
            System.err.println("--canvas is required");
            return 2;
        }
        var folder = FoldWiring.canvasFolder();
        var canvas = folder.parse(Files.readString(flags.canvasPath, StandardCharsets.UTF_8));
        var model = folder.fold(canvas);
        var verificationPlan = FoldWiring.planner().plan(model);
        Files.createDirectories(flags.outDir);
        var jsonPath = flags.outDir.resolve(canvas.workId() + ".plan.json");
        var payload = new LinkedHashMap<String, Object>();
        payload.put("workId", canvas.workId());
        payload.put("readyForImplementation", verificationPlan.readyForImplementation());
        payload.put("ruleCount", verificationPlan.rules().size());
        payload.put("missingObligations", verificationPlan.missingObligations());
        payload.put("rules", verificationPlan.rules());
        mapper().writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), payload);
        System.out.println("plan workId=" + canvas.workId());
        System.out.println("readyForImplementation=" + verificationPlan.readyForImplementation());
        System.out.println("rules=" + verificationPlan.rules().size());
        System.out.print(SemanticModelRenderer.render(verificationPlan.model()));
        System.out.println("wrote " + jsonPath);
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
