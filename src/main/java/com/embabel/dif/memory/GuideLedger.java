package com.embabel.dif.memory;

import com.embabel.dif.domain.SemanticModel;

/**
 * Optional Guide/DICE projection. Retrieval may quote these nodes;
 * it does not own or freeze the fold.
 * // Retrieve past beliefs; do not freeze them: Guide DICE
 */
public final class GuideLedger {

    private GuideLedger() {
    }

    public static String jsonl(String workId, SemanticModel model) {
        var lines = new StringBuilder();
        for (var invariant : model.invariants()) {
            lines.append(node("Decision", workId, invariant.description(), invariant.id())).append('\n');
        }
        for (var conflict : model.conflicts()) {
            lines.append(node("Pitfall", workId, conflict.explanation(), "conflict")).append('\n');
        }
        for (var obligation : model.missingObligations()) {
            lines.append(node("Pitfall", workId, "Missing: " + obligation.obligation(), obligation.derivedFromIntent()))
                    .append('\n');
        }
        return lines.toString();
    }

    private static String node(String kind, String workId, String text, String source) {
        return "{\"kind\":\"" + escape(kind)
                + "\",\"workId\":\"" + escape(workId)
                + "\",\"text\":\"" + escape(text)
                + "\",\"source\":\"" + escape(source)
                + "\"}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
