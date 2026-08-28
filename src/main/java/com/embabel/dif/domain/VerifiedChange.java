package com.embabel.dif.domain;

/**
 * Phase 4/5 goal type: a proposed change that passed semantic verification.
 */
public record VerifiedChange(
        ProposedChange change,
        SemanticVerification verification
) {
}
