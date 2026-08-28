package com.embabel.dif.agent;

import com.embabel.agent.api.common.Ai;
import com.embabel.dif.domain.ProposedChange;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.SemanticVerification;
import com.embabel.dif.domain.TestExecution;
import com.embabel.dif.domain.VerificationFailure;
import com.embabel.dif.domain.VerifiedChange;
import com.embabel.dif.verifier.SemanticVerifier;
import org.springframework.stereotype.Component;

/**
 * Phase 3–5 stubs. Not Embabel {@code @Action} methods yet so GOAP stays on the
 * milestone-1 path: ChangeRequest → CandidateIntent → SemanticModel → VerificationPlan.
 */
@Component
public class LaterPhaseActions {

    private final SemanticVerifier verifier;

    public LaterPhaseActions(SemanticVerifier verifier) {
        this.verifier = verifier;
    }

    public ProposedChange generateChange(SemanticModel semanticModel, RepositoryAnalysis analysis, Ai ai) {
        throw new UnsupportedOperationException("Phase 3: LLM implementation generation is not wired yet");
    }

    public TestExecution runTests(ProposedChange change) {
        throw new UnsupportedOperationException("Phase 4: test execution is not wired yet");
    }

    public SemanticVerification verify(SemanticModel model, ProposedChange change, TestExecution tests) {
        throw new UnsupportedOperationException("Phase 4: full verification against generated code is not wired yet");
    }

    public VerifiedChange accept(ProposedChange change, SemanticVerification verification) {
        if (!verification.passed()) {
            throw new IllegalStateException("Semantic verification failed");
        }
        return new VerifiedChange(change, verification);
    }

    public VerificationFailure failureOf(SemanticVerification verification) {
        return new VerificationFailure(
                verification.results().stream()
                        .filter(result -> result.status().name().equals("FAIL"))
                        .toList()
        );
    }

    SemanticVerifier verifier() {
        return verifier;
    }
}
