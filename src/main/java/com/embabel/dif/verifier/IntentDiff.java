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
    public IntentDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        unchanged = List.copyOf(unchanged);
    }

    public boolean passed() {
        return true;
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
