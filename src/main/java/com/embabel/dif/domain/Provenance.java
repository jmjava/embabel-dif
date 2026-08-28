package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Where a semantic assertion came from")
public record Provenance(
        EvidenceType type,
        String source,
        String detail
) {
    public static Provenance of(EvidenceType type, String source) {
        return new Provenance(type, source, "");
    }
}
