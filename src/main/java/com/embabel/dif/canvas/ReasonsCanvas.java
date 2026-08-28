package com.embabel.dif.canvas;

import java.util.List;

/**
 * Parsed REASONS Canvas. The markdown file remains the source of truth;
 * this is only the structured view used to fold.
 */
public record ReasonsCanvas(
        String workId,
        String title,
        String readiness,
        List<String> acceptanceCriteria,
        List<String> nonGoals,
        List<String> assumptions,
        List<String> safeguards,
        List<String> norms,
        List<String> entities,
        List<String> filesLikelyAffected,
        List<CanvasOperation> operations
) {
    public ReasonsCanvas {
        acceptanceCriteria = List.copyOf(acceptanceCriteria);
        nonGoals = List.copyOf(nonGoals);
        assumptions = List.copyOf(assumptions);
        safeguards = List.copyOf(safeguards);
        norms = List.copyOf(norms);
        entities = List.copyOf(entities);
        filesLikelyAffected = List.copyOf(filesLikelyAffected);
        operations = List.copyOf(operations);
    }
}
