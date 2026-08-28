package com.embabel.dif.dif;

import com.embabel.dif.domain.ConflictReason;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentConflict;

import java.util.Optional;

/**
 * Alloy-shaped exclusive predicates evaluated in-process.
 * // counterexample search: Alloy
 * // vacuity: intent-lang
 *
 * Token overlap cannot see these pairs. No Z3/Alloy binary is required;
 * {@link AlloyModelEmitter} writes the same facts for inspection.
 */
public class FormalConflictBackend {

    public Optional<IntentConflict> detect(Intent left, Intent right) {
        var a = ConflictDetector.normalize(left.statement());
        var b = ConflictDetector.normalize(right.statement());
        if (exactlyOnce(a) && duplicatesOk(b) || exactlyOnce(b) && duplicatesOk(a)) {
            return Optional.of(quoted(
                    left,
                    right,
                    "Formal vacuity: exactly-once delivery is mutually exclusive with acceptable duplicates"
            ));
        }
        if (appendOnly(a) && deletesAllowed(b) || appendOnly(b) && deletesAllowed(a)) {
            return Optional.of(quoted(
                    left,
                    right,
                    "Formal vacuity: append-only is mutually exclusive with allowed deletes"
            ));
        }
        return Optional.empty();
    }

    static boolean exactlyOnce(String statement) {
        return statement.contains("exactly-once")
                || statement.contains("exactly once")
                || statement.contains("at-most-once")
                || statement.contains("at most once");
    }

    static boolean duplicatesOk(String statement) {
        return statement.contains("duplicates are acceptable")
                || statement.contains("duplicate deliveries are ok")
                || statement.contains("at-least-once is sufficient")
                || statement.contains("at least once is sufficient");
    }

    static boolean appendOnly(String statement) {
        return statement.contains("append-only") || statement.contains("append only");
    }

    static boolean deletesAllowed(String statement) {
        return statement.contains("may delete")
                || statement.contains("deletes are allowed")
                || statement.contains("clients may delete");
    }

    private static IntentConflict quoted(Intent left, Intent right, String rule) {
        return new IntentConflict(
                left,
                right,
                ConflictReason.MUTUALLY_EXCLUSIVE,
                rule + ": \"" + left.statement() + "\" vs \"" + right.statement() + "\""
        );
    }
}
