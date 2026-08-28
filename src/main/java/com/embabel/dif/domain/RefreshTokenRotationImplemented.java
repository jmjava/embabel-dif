package com.embabel.dif.domain;

/**
 * Phase 4+ use-case goal from the prototype spec. Not wired as an Embabel goal yet.
 */
public record RefreshTokenRotationImplemented(
        ProposedChange change,
        SemanticVerification verificationReport
) {
}
