package com.embabel.dif.agent;

import com.embabel.agent.api.common.Ai;
import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.ChangeRequest;

public interface IntentInterpreter {

    boolean supports(ChangeRequest request);

    CandidateIntent interpret(ChangeRequest request, Ai ai);
}
