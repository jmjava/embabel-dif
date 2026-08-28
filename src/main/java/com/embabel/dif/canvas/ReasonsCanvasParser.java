package com.embabel.dif.canvas;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic REASONS markdown parser. Unknown sections are ignored.
 */
@Component
public class ReasonsCanvasParser {

    private static final Pattern METADATA = Pattern.compile("^-\\s+([^:]+):\\s*(.*)$");
    private static final Pattern OPERATION = Pattern.compile("^###\\s+(T\\d+)\\s*-\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BULLET = Pattern.compile("^-\\s+(?:\\[[ xX]\\]\\s+)?(.+)$");

    public ReasonsCanvas parse(String markdown) {
        var metadata = new LinkedHashMap<String, String>();
        var acceptance = new ArrayList<String>();
        var nonGoals = new ArrayList<String>();
        var assumptions = new ArrayList<String>();
        var safeguards = new ArrayList<String>();
        var norms = new ArrayList<String>();
        var entities = new ArrayList<String>();
        var files = new ArrayList<String>();
        var operations = new ArrayList<CanvasOperation>();

        String h2 = "";
        String h3 = "";
        String title = "";
        CanvasOperationBuilder currentOp = null;

        for (var raw : markdown.split("\\R", -1)) {
            var line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                title = line.substring(2).trim();
                continue;
            }
            if (line.startsWith("### ")) {
                flushOperation(operations, currentOp);
                var opMatch = OPERATION.matcher(line);
                if (opMatch.matches() && h2.equals("operations")) {
                    currentOp = new CanvasOperationBuilder(opMatch.group(1).toUpperCase(Locale.ROOT), opMatch.group(2).trim());
                } else {
                    currentOp = null;
                    h3 = headingKey(line);
                }
                continue;
            }
            if (line.startsWith("## ")) {
                flushOperation(operations, currentOp);
                currentOp = null;
                h2 = headingKey(line);
                h3 = "";
                continue;
            }

            if (currentOp != null) {
                currentOp.readDetail(line);
                continue;
            }

            if (h2.isEmpty()) {
                var meta = METADATA.matcher(line);
                if (meta.matches()) {
                    metadata.put(normalizeKey(meta.group(1)), meta.group(2).trim());
                }
                continue;
            }

            var item = bulletText(line);
            if (item == null) {
                continue;
            }
            switch (section(h2, h3)) {
                case "acceptance" -> acceptance.add(item);
                case "nongoals" -> nonGoals.add(item);
                case "assumptions" -> assumptions.add(item);
                case "safeguards" -> safeguards.add(item);
                case "norms" -> norms.add(item);
                case "entities" -> entities.add(item);
                case "files" -> files.add(stripTicks(item));
                default -> {
                }
            }
        }
        flushOperation(operations, currentOp);

        var workId = firstNonBlank(metadata.get("work id"), workIdFromTitle(title), "UNKNOWN");
        return new ReasonsCanvas(
                workId,
                title,
                firstNonBlank(metadata.get("readiness"), metadata.get("status"), ""),
                acceptance,
                nonGoals,
                assumptions,
                safeguards,
                norms,
                entities,
                files,
                operations
        );
    }

    private static void flushOperation(List<CanvasOperation> operations, CanvasOperationBuilder builder) {
        if (builder != null) {
            operations.add(builder.build());
        }
    }

    private static String section(String h2, String h3) {
        if (h2.contains("safeguard")) {
            return "safeguards";
        }
        if (h2.contains("norm")) {
            return "norms";
        }
        if (h3.contains("acceptance")) {
            return "acceptance";
        }
        if (h3.contains("non-goal") || h3.contains("nongoal")) {
            return "nongoals";
        }
        if (h3.contains("assumption")) {
            return "assumptions";
        }
        if (h3.contains("domain entit") || h3.equals("entities")) {
            return "entities";
        }
        if (h3.contains("files likely") || h3.contains("files to modify") || h3.contains("files to add")) {
            return "files";
        }
        return "";
    }

    private static String headingKey(String line) {
        var text = line.replaceFirst("^#+\\s*", "").toLowerCase(Locale.ROOT).trim();
        return text.replaceFirst("^[reasonsotn]\\s+-\\s+", "").trim();
    }

    private static String bulletText(String line) {
        Matcher matcher = BULLET.matcher(line);
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private static String stripTicks(String value) {
        return value.replace("`", "").trim();
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String workIdFromTitle(String title) {
        var matcher = Pattern.compile("(FEAT|BUG|SPIKE|CHORE)-[A-Z0-9-]+", Pattern.CASE_INSENSITIVE).matcher(title);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    private static String firstNonBlank(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static final class CanvasOperationBuilder {
        private final String id;
        private final String name;
        private String status = "";
        private String description = "";

        private CanvasOperationBuilder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private void readDetail(String line) {
            var meta = METADATA.matcher(line);
            if (!meta.matches()) {
                return;
            }
            var key = normalizeKey(meta.group(1));
            var value = meta.group(2).trim();
            if (key.equals("status")) {
                status = value;
            } else if (key.equals("description")) {
                description = value;
            }
        }

        private CanvasOperation build() {
            return new CanvasOperation(id, name, status, description);
        }
    }
}
