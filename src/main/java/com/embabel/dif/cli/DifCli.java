package com.embabel.dif.cli;

import com.embabel.dif.canvas.CanvasFolder;
import com.embabel.dif.canvas.CanvasIntentMapper;
import com.embabel.dif.canvas.ReasonsCanvasParser;
import com.embabel.dif.dif.ConflictDetector;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.dif.RuleBasedIntentFolder;
import com.embabel.dif.memory.SemanticModelRenderer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Standalone fold entry point. Does not start Embabel or Spring.
 *
 * <pre>
 *   ./scripts/dif-fold.sh --canvas examples/canvases/FEAT-001-order-status-api.md
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
                      fold --canvas <file> [--out <dir>]
                    
                    Reads a REASONS Canvas and writes a deterministic SemanticModel
                    projection. Exit 1 if blocking intent conflicts exist.
                    """);
            return args.length == 0 ? 2 : 0;
        }
        if (!"fold".equals(args[0])) {
            System.err.println("Unknown command: " + args[0]);
            return 2;
        }

        Path canvasPath = null;
        var outDir = Path.of(".dif/projections");
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--canvas" -> canvasPath = Path.of(requireValue(args, ++i, "--canvas"));
                case "--out" -> outDir = Path.of(requireValue(args, ++i, "--out"));
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    return 2;
                }
            }
        }
        if (canvasPath == null) {
            System.err.println("--canvas is required");
            return 2;
        }

        var markdown = Files.readString(canvasPath, StandardCharsets.UTF_8);
        var folder = newFolder();
        var canvas = folder.parse(markdown);
        var model = folder.fold(canvas);
        var projection = new FoldProjection(
                canvas.workId(),
                canvasPath.toString(),
                !model.hasBlockingConflicts(),
                model
        );

        Files.createDirectories(outDir);
        var jsonPath = outDir.resolve(canvas.workId() + ".json");
        var textPath = outDir.resolve(canvas.workId() + ".txt");
        mapper().writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), projection);
        Files.writeString(textPath, SemanticModelRenderer.render(model), StandardCharsets.UTF_8);

        System.out.print(SemanticModelRenderer.render(model));
        System.out.println("wrote " + jsonPath);
        return model.hasBlockingConflicts() ? 1 : 0;
    }

    private static CanvasFolder newFolder() {
        return new CanvasFolder(
                new ReasonsCanvasParser(),
                new CanvasIntentMapper(),
                new RuleBasedIntentFolder(new ConflictDetector()),
                new ObligationDeriver()
        );
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[index];
    }
}
