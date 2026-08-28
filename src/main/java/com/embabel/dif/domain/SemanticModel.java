package com.embabel.dif.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Canonical folded semantic state. Lists are stored in stable order so the same
 * accepted facts always produce an equal model.
 */
public record SemanticModel(
        List<Intent> intents,
        List<Invariant> invariants,
        List<SemanticRelation> relations,
        List<IntentConflict> conflicts,
        List<MissingObligation> missingObligations
) {
    public SemanticModel {
        intents = List.copyOf(intents);
        invariants = List.copyOf(invariants);
        relations = List.copyOf(relations);
        conflicts = List.copyOf(conflicts);
        missingObligations = List.copyOf(missingObligations);
    }

    public SemanticModel(List<Intent> intents, List<Invariant> invariants, List<SemanticRelation> relations) {
        this(intents, invariants, relations, List.of(), List.of());
    }

    public boolean hasBlockingConflicts() {
        return conflicts.stream().anyMatch(IntentConflict::blocking);
    }

    public SemanticModel withMissingObligations(List<MissingObligation> obligations) {
        return new SemanticModel(intents, invariants, relations, conflicts, canonicalObligations(obligations));
    }

    public static List<Intent> canonicalIntents(List<Intent> intents) {
        return intents.stream().sorted(Comparator.comparing(Intent::id)).toList();
    }

    public static List<Invariant> canonicalInvariants(List<Invariant> invariants) {
        return invariants.stream().sorted(Comparator.comparing(Invariant::id)).toList();
    }

    public static List<SemanticRelation> canonicalRelations(List<SemanticRelation> relations) {
        return relations.stream()
                .sorted(Comparator
                        .comparing((SemanticRelation relation) -> relation.from().id())
                        .thenComparing(relation -> relation.type().name())
                        .thenComparing(relation -> relation.to().id()))
                .toList();
    }

    public static List<IntentConflict> canonicalConflicts(List<IntentConflict> conflicts) {
        return conflicts.stream()
                .sorted(Comparator
                        .comparing((IntentConflict conflict) -> conflict.left().id())
                        .thenComparing(conflict -> conflict.right().id()))
                .toList();
    }

    public static List<MissingObligation> canonicalObligations(List<MissingObligation> obligations) {
        return obligations.stream()
                .sorted(Comparator
                        .comparing(MissingObligation::derivedFromIntent)
                        .thenComparing(MissingObligation::obligation))
                .toList();
    }
}
