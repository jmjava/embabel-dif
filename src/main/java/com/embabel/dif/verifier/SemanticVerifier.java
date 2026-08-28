package com.embabel.dif.verifier;

import com.embabel.dif.domain.ProposedChange;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.SemanticSnapshot;
import com.embabel.dif.domain.SemanticVerification;
import com.embabel.dif.domain.TestExecution;

/**
 * Deterministic acceptance boundary. Must not ask an LLM whether a change "looks correct".
 */
public interface SemanticVerifier {

    IntentDiff diff(SemanticSnapshot before, SemanticSnapshot desired);

    SemanticVerification verify(
            SemanticModel model,
            ProposedChange change,
            TestExecution tests,
            IntentDiff intentDiff
    );
}
