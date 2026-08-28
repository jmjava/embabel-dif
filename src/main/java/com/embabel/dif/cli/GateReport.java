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

    /**
     * One-line gate for orch attach. Matches {@code dif=skipped} from
     * {@code check-canvas.sh} when the CLI is absent.
     */
    public String oneLine() {
        return oneLine(workId, readyForImplementation, blockingConflicts.size());
    }

    public static String oneLine(String workId, boolean ready, int conflictCount) {
        if (ready) {
            return "dif=ready workId=" + workId + " readyForImplementation=true";
        }
        return "dif=blocked workId=" + workId
                + " readyForImplementation=false conflicts=" + conflictCount;
    }

    public record BlockingConflict(String left, String right, String explanation) {
    }
}
