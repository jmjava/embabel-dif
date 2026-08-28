package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.Set;

@JsonClassDescription("A system property that must hold after a change")
public record Invariant(
        String id,
        String description,
        VerificationStrategy strategy,
        Set<String> relatedIntentIds
) {
    public Invariant {
        relatedIntentIds = Set.copyOf(relatedIntentIds);
    }
}
