package com.embabel.dif.memory;

import com.embabel.dif.domain.SemanticModel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File-backed memory placeholder. Writes a inspectable snapshot; full YAML split comes later.
 */
@Component
public class FileSemanticMemory implements SemanticMemory {

    @Override
    public void save(SemanticModel model, Path root) throws IOException {
        Files.createDirectories(root.resolve("intents"));
        Files.createDirectories(root.resolve("invariants"));
        Files.createDirectories(root.resolve("evidence"));
        Files.createDirectories(root.resolve("relations"));
        Files.createDirectories(root.resolve("snapshots"));
        var snapshot = root.resolve("snapshots/latest.txt");
        Files.writeString(snapshot, render(model), StandardCharsets.UTF_8);
    }

    @Override
    public SemanticModel load(Path root) {
        throw new UnsupportedOperationException("Phase 6: load from .dif/ is not implemented yet");
    }

    private static String render(SemanticModel model) {
        var text = new StringBuilder();
        text.append("intents:\n");
        model.intents().forEach(intent ->
                text.append("  - ").append(intent.id()).append(": ").append(intent.statement()).append('\n'));
        text.append("invariants:\n");
        model.invariants().forEach(invariant ->
                text.append("  - ").append(invariant.id()).append(": ").append(invariant.description()).append('\n'));
        text.append("conflicts:\n");
        model.conflicts().forEach(conflict ->
                text.append("  - ").append(conflict.left().id()).append(" CONFLICTS_WITH ")
                        .append(conflict.right().id()).append('\n'));
        text.append("missingObligations:\n");
        model.missingObligations().forEach(obligation ->
                text.append("  - ").append(obligation.derivedFromIntent()).append(": ")
                        .append(obligation.obligation()).append('\n'));
        return text.toString();
    }
}
