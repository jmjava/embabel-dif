package com.embabel.dif.domain;

public record SemanticRelation(
        SemanticNode from,
        RelationType type,
        SemanticNode to,
        Provenance provenance
) {
}
