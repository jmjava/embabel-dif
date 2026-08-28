package com.embabel.dif.agent;

import com.embabel.agent.api.common.Ai;
import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.ChangeRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Probabilistic front end: LLM extracts candidate intents and evidence.
 */
@Component
@Order(10)
public class LlmIntentInterpreter implements IntentInterpreter {

    @Override
    public boolean supports(ChangeRequest request) {
        return true;
    }

    @Override
    public CandidateIntent interpret(ChangeRequest request, Ai ai) {
        return ai.withAutoLlm()
                .creating(CandidateIntent.class)
                .fromPrompt("""
                        Extract a CandidateIntent from this software change request.
                        Produce explicit Intent records (REQUIREMENT or PRESERVATION) and Evidence records.
                        Do not implement the change. Do not invent repository facts you were not given.
                        
                        # Change request
                        %s
                        """.formatted(request.text()).trim());
    }
}
