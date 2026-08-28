package com.embabel.dif.verifier;

import com.embabel.dif.domain.SemanticProperty;

import java.util.List;
import java.util.Set;

/**
 * Semantic equivalent of {@code git diff}: behavior added, removed, or preserved.
 */
public record IntentDiff(
        List<SemanticProperty> added,
        List<SemanticProperty> removed,
        List<SemanticProperty> unchanged
) {
    /**
     * Login/JWT paths the prototype must not drop. A review of snapshots that
     * never contained these keys still {@link #passed() passes}; only removal
     * (or a value change, which appears in {@code removed}) fails.
     */
    public static final Set<String> DEFAULT_REQUIRED_PATHS = Set.of(
            "provider.GOOGLE",
            "provider.APPLE",
            "jwt.claim.sessionToken"
    );

    public IntentDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        unchanged = List.copyOf(unchanged);
    }

    /**
     * Fail closed when a required safeguard path was removed or changed.
     * Stricter “must still be present” is {@link #preserves(Set)}.
     */
    public boolean passed() {
        return passed(DEFAULT_REQUIRED_PATHS);
    }

    public boolean passed(Set<String> requiredPaths) {
        var removedPaths = removed.stream().map(SemanticProperty::path).toList();
        return requiredPaths.stream().noneMatch(removedPaths::contains);
    }

    public boolean preserves(Set<String> requiredPaths) {
        var unchangedPaths = unchanged.stream().map(SemanticProperty::path).toList();
        var removedPaths = removed.stream().map(SemanticProperty::path).toList();
        return requiredPaths.stream().allMatch(path ->
                unchangedPaths.contains(path) && !removedPaths.contains(path));
    }

    public String render() {
        var lines = new StringBuilder("SEMANTIC DIFF\n");
        added.forEach(property -> lines.append("+ ").append(property.path()).append('=').append(property.value()).append('\n'));
        removed.forEach(property -> lines.append("- ").append(property.path()).append('=').append(property.value()).append('\n'));
        lines.append("\nUNCHANGED:\n");
        unchanged.forEach(property -> lines.append("= ").append(property.path()).append('=').append(property.value()).append('\n'));
        lines.append("\nRESULT: ").append(passed() ? "PASS" : "FAIL");
        return lines.toString().trim();
    }
}
