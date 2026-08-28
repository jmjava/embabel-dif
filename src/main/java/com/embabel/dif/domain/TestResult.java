package com.embabel.dif.domain;

public record TestResult(
        String name,
        boolean passed,
        String detail
) {
}
