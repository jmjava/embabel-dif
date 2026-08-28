package com.embabel.dif.dif;

import com.embabel.dif.domain.ConflictReason;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentConflict;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds mutually exclusive accepted intents before implementation proceeds.
 */
@Component
public class ConflictDetector {

    public List<IntentConflict> detect(List<Intent> intents) {
        var conflicts = new ArrayList<IntentConflict>();
        for (int i = 0; i < intents.size(); i++) {
            for (int j = i + 1; j < intents.size(); j++) {
                detectPair(intents.get(i), intents.get(j)).ifPresent(conflicts::add);
            }
        }
        return List.copyOf(conflicts);
    }

    private java.util.Optional<IntentConflict> detectPair(Intent left, Intent right) {
        var a = normalize(left.statement());
        var b = normalize(right.statement());
        if (singleUse(a) && reusable(b) || singleUse(b) && reusable(a)) {
            return java.util.Optional.of(new IntentConflict(
                    left,
                    right,
                    ConflictReason.MUTUALLY_EXCLUSIVE,
                    "Refresh-token single-use/rotation conflicts with indefinite reuse"
            ));
        }
        return java.util.Optional.empty();
    }

    private static boolean singleUse(String statement) {
        return statement.contains("single-use")
                || statement.contains("rotate")
                || statement.contains("cannot be reused")
                || statement.contains("must never authenticate again")
                || statement.contains("consumed refresh token");
    }

    private static boolean reusable(String statement) {
        return statement.contains("reuse the same refresh token")
                || statement.contains("remain valid after use")
                || statement.contains("reusable indefinitely")
                || statement.contains("indefinitely");
    }

    static String normalize(String statement) {
        return statement.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
