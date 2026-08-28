package com.embabel.dif.cli;

import com.embabel.dif.domain.IntentConflict;
import com.embabel.dif.domain.MissingObligation;
import com.embabel.dif.domain.SemanticModel;

import java.util.List;

/**
 * Stable machine contract for orchestrator attach. A Python helper can decide
 * Ready For Coding from this file without parsing fold stdout.
 */
public record GateReport(
        String workId,
        boolean readyForImplementation,
        List<BlockingConflict> blockingConflicts,
        List<String> missingObligations
) {
    public GateReport {
        blockingConflicts = List.copyOf(blockingConflicts);
        missingObligations = List.copyOf(missingObligations);
    }

    public static GateReport from(String workId, SemanticModel model) {
        var conflicts = model.conflicts().stream()
                .filter(IntentConflict::blocking)
                .map(conflict -> new BlockingConflict(
                        conflict.left().statement(),
                        conflict.right().statement(),
                        conflict.explanation()
                ))
                .toList();
        var missing = model.missingObligations().stream()
                .map(MissingObligation::obligation)
                .toList();
        return new GateReport(workId, !model.hasBlockingConflicts(), conflicts, missing);
    }

    public record BlockingConflict(String left, String right, String explanation) {
    }
}
