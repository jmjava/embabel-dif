package com.embabel.dif.domain;

public record SemanticNode(
        String id,
        SemanticNodeKind kind,
        String label
) {
    public static SemanticNode intent(Intent intent) {
        return new SemanticNode(intent.id(), SemanticNodeKind.INTENT, intent.statement());
    }

    public static SemanticNode invariant(Invariant invariant) {
        return new SemanticNode(invariant.id(), SemanticNodeKind.INVARIANT, invariant.description());
    }

    public static SemanticNode evidence(Evidence evidence) {
        return new SemanticNode(evidence.id(), SemanticNodeKind.EVIDENCE, evidence.observation());
    }
}
