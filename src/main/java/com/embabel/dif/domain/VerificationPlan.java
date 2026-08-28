package com.embabel.dif.domain;

import com.embabel.dif.verifier.VerificationRule;

import java.util.List;

/**
 * Milestone-1 goal: folded semantics plus the deterministic checks that must run later.
 */
public record VerificationPlan(
        SemanticModel model,
        RepositoryAnalysis analysis,
        List<VerificationRule> rules,
        List<MissingObligation> missingObligations
) {
    public VerificationPlan {
        rules = List.copyOf(rules);
        missingObligations = List.copyOf(missingObligations);
    }

    public boolean readyForImplementation() {
        return !model.hasBlockingConflicts();
    }
}
