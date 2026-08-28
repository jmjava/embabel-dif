package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;

@JsonClassDescription("LLM-proposed intents and supporting evidence, not yet folded")
public record CandidateIntent(
        List<Intent> intents,
        List<Evidence> evidence
) {
    public CandidateIntent {
        intents = List.copyOf(intents);
        evidence = List.copyOf(evidence);
    }
}
