package com.embabel.dif.domain;

import java.util.List;

/**
 * Typed failure fact for Embabel re-planning (Phase 5).
 */
public record VerificationFailure(List<VerificationResult> failures) {
    public VerificationFailure {
        failures = List.copyOf(failures);
    }
}
