package com.embabel.dif.domain;

import java.util.Optional;

public record VerificationResult(
        String invariantId,
        VerificationStatus status,
        String evidence,
        Optional<String> failureReason
) {
    public static VerificationResult pass(String invariantId, String evidence) {
        return new VerificationResult(invariantId, VerificationStatus.PASS, evidence, Optional.empty());
    }

    public static VerificationResult fail(String invariantId, String evidence, String reason) {
        return new VerificationResult(invariantId, VerificationStatus.FAIL, evidence, Optional.of(reason));
    }
}
