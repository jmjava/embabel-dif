package com.embabel.dif;

import com.embabel.dif.canvas.CanvasFolder;
import com.embabel.dif.canvas.CanvasIntentMapper;
import com.embabel.dif.canvas.ReasonsCanvasParser;
import com.embabel.dif.dif.ConflictDetector;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.dif.RuleBasedIntentFolder;
import com.embabel.dif.dif.VerificationPlanner;

/**
 * Embabel-free wiring for the canvas CLI and contract tests.
 */
public final class FoldWiring {

    private FoldWiring() {
    }

    public static CanvasFolder canvasFolder() {
        return new CanvasFolder(
                new ReasonsCanvasParser(),
                new CanvasIntentMapper(),
                new RuleBasedIntentFolder(new ConflictDetector()),
                new ObligationDeriver()
        );
    }

    public static VerificationPlanner planner() {
        return new VerificationPlanner(new ObligationDeriver());
    }
}
