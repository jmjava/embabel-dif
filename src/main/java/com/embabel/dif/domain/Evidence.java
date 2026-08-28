package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("An observation that supports or challenges intent")
public record Evidence(
        String id,
        EvidenceType type,
        String source,
        String observation
) {
}
