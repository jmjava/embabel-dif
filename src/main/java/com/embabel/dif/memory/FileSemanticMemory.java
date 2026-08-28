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
        Files.writeString(snapshot, SemanticModelRenderer.render(model), StandardCharsets.UTF_8);
    }

    @Override
    public SemanticModel load(Path root) {
        throw new UnsupportedOperationException("Phase 6: load from .dif/ is not implemented yet");
    }
}
