package com.embabel.dif.dif;

import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.SemanticModel;

/**
 * Emits an Alloy sketch from an already-folded model.
 * Humans do not write {@code .als}; the canvas stays the source of truth.
 * // counterexample search: Alloy
 */
public final class AlloyModelEmitter {

    private AlloyModelEmitter() {
    }

    public static String emit(String workId, SemanticModel model) {
        var text = new StringBuilder();
        text.append("module ").append(sanitize(workId)).append('\n');
        text.append("// Generated from a folded SemanticModel. Do not edit.\n");
        text.append("// counterexample search: Alloy — evaluated in-process by FormalConflictBackend.\n\n");
        text.append("abstract sig Predicate {}\n");
        text.append("one sig ExactlyOnce, DuplicatesOk, AppendOnly, DeletesAllowed extends Predicate {}\n");
        text.append("sig Active in Predicate {}\n\n");
        text.append("fact Vacuity {\n");
        text.append("  not (ExactlyOnce in Active and DuplicatesOk in Active)\n");
        text.append("  not (AppendOnly in Active and DeletesAllowed in Active)\n");
        text.append("}\n\n");
        text.append("pred show {}\n");
        text.append("run show for 3\n\n");
        text.append("// Intents considered:\n");
        for (var intent : model.intents()) {
            text.append("// ").append(tag(intent)).append(' ').append(intent.statement()).append('\n');
        }
        return text.toString();
    }

    private static String tag(Intent intent) {
        var statement = ConflictDetector.normalize(intent.statement());
        if (FormalConflictBackend.exactlyOnce(statement)) {
            return "[ExactlyOnce]";
        }
        if (FormalConflictBackend.duplicatesOk(statement)) {
            return "[DuplicatesOk]";
        }
        if (FormalConflictBackend.appendOnly(statement)) {
            return "[AppendOnly]";
        }
        if (FormalConflictBackend.deletesAllowed(statement)) {
            return "[DeletesAllowed]";
        }
        return "[uninterpreted]";
    }

    private static String sanitize(String workId) {
        return workId.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
