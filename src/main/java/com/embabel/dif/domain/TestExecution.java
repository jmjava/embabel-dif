package com.embabel.dif.domain;

import java.util.List;

/**
 * Phase 4 stub: build/test execution against a proposed change.
 */
public record TestExecution(
        boolean passed,
        List<TestResult> results
) {
    public TestExecution {
        results = List.copyOf(results);
    }
}
