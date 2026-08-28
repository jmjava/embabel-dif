package com.embabel.dif.agent;

import com.embabel.agent.api.common.Ai;
import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.ChangeRequest;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Deterministic interpreter for the prototype refresh-token scenario.
 * Lets milestone 1 run without an LLM key.
 */
@Component
@Order(0)
public class FixtureIntentInterpreter implements IntentInterpreter {

    @Override
    public boolean supports(ChangeRequest request) {
        return RefreshTokenScenario.matches(request);
    }

    @Override
    public CandidateIntent interpret(ChangeRequest request, Ai ai) {
        return RefreshTokenScenario.candidateIntent();
    }
}
