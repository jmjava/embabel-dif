package com.embabel.dif.dif;

import com.embabel.dif.domain.ConflictReason;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentConflict;
import com.embabel.dif.domain.IntentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds mutually exclusive accepted intents before implementation proceeds.
 */
@Component
public class ConflictDetector {

    private static final Set<String> STOP_WORDS = Set.of(
            "that", "this", "with", "from", "must", "remain", "change", "changes",
            "existing", "without", "unless", "required", "behavior", "unrelated",
            "do", "not", "the", "and", "for", "into", "goal", "non", "preserve"
    );

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
        if (requirementVsNonGoal(left, right) && sharesSignificantToken(a, b)) {
            return java.util.Optional.of(new IntentConflict(
                    left,
                    right,
                    ConflictReason.MUTUALLY_EXCLUSIVE,
                    "Requirement conflicts with a canvas non-goal"
            ));
        }
        return java.util.Optional.empty();
    }

    private static boolean requirementVsNonGoal(Intent left, Intent right) {
        return isRequirement(left) && isNonGoal(right) || isRequirement(right) && isNonGoal(left);
    }

    private static boolean isRequirement(Intent intent) {
        return intent.type() == IntentType.REQUIREMENT || intent.type() == IntentType.GOAL;
    }

    private static boolean isNonGoal(Intent intent) {
        return intent.type() == IntentType.CONSTRAINT;
    }

    private static boolean sharesSignificantToken(String left, String right) {
        return relatedTokens(significantTokens(left), significantTokens(right));
    }

    static Set<String> significantTokens(String statement) {
        return Arrays.stream(normalize(statement).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 4)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private static boolean relatedTokens(Set<String> left, Set<String> right) {
        for (var a : left) {
            for (var b : right) {
                if (a.equals(b)) {
                    return true;
                }
                if (a.length() >= 6 && b.length() >= 6 && commonPrefixLength(a, b) >= 6) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int commonPrefixLength(String left, String right) {
        var n = Math.min(left.length(), right.length());
        var i = 0;
        while (i < n && left.charAt(i) == right.charAt(i)) {
            i++;
        }
        return i;
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
