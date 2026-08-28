package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("An accepted or candidate statement of what should be true")
public record Intent(
        String id,
        IntentType type,
        String statement,
        Priority priority,
        Provenance provenance
) {
}
