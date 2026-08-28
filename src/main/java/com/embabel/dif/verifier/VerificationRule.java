package com.embabel.dif.verifier;

import com.embabel.dif.domain.VerificationStrategy;

public record VerificationRule(
        String invariantId,
        VerificationStrategy strategy,
        String description
) {
}
