package com.embabel.dif.cli;

import com.embabel.dif.domain.SemanticModel;

public record FoldProjection(
        String workId,
        String canvasPath,
        boolean readyForImplementation,
        SemanticModel model
) {
}
