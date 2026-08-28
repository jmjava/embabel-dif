package com.embabel.dif.memory;

import com.embabel.dif.domain.SemanticModel;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Phase 6 stub: persist accepted semantic objects under {@code .dif/}.
 */
public interface SemanticMemory {

    void save(SemanticModel model, Path root) throws IOException;

    SemanticModel load(Path root) throws IOException;
}
