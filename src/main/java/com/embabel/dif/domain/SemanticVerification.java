package com.embabel.dif.domain;

import java.util.List;

public record SemanticVerification(
        boolean passed,
        List<VerificationResult> results
) {
    public SemanticVerification {
        results = List.copyOf(results);
    }
}
