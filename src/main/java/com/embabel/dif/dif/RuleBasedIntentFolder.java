package com.embabel.dif.dif;

import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.Evidence;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.Invariant;
import com.embabel.dif.domain.Priority;
import com.embabel.dif.domain.Provenance;
import com.embabel.dif.domain.RelationType;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.SemanticNode;
import com.embabel.dif.domain.SemanticRelation;
import com.embabel.dif.domain.VerificationStrategy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic fold: normalize, deduplicate, derive invariants, relate, detect conflicts.
 * No novel mathematical folding algorithm — rule-based MVP from the prototype spec.
 * Grow the catalog here; see docs/FOLD_ITERATION.md.
 * // requires/ensures as named contracts: lhaig/intent, kodo
 */
@Component
public class RuleBasedIntentFolder implements IntentFolder {

    private final ConflictDetector conflictDetector;

    public RuleBasedIntentFolder(ConflictDetector conflictDetector) {
        this.conflictDetector = conflictDetector;
    }

    @Override
    public SemanticModel fold(CandidateIntent candidate) {
        var intents = canonicalIntents(candidate.intents());
        var evidence = canonicalEvidence(candidate.evidence());
        var invariants = deriveInvariants(intents);
        var relations = new ArrayList<SemanticRelation>();
        relations.addAll(evidenceRelations(intents, evidence));
        relations.addAll(invariantRelations(intents, invariants));
        relations.addAll(conflictRelations(intents));

        var conflicts = conflictDetector.detect(intents);
        return new SemanticModel(
                SemanticModel.canonicalIntents(intents),
                SemanticModel.canonicalInvariants(invariants),
                SemanticModel.canonicalRelations(relations),
                SemanticModel.canonicalConflicts(conflicts),
                List.of()
        );
    }

    private List<Intent> canonicalIntents(List<Intent> incoming) {
        var byKey = new LinkedHashMap<String, Intent>();
        int generated = 1;
        for (var intent : incoming) {
            var statement = collapse(intent.statement());
            var key = ConflictDetector.normalize(statement);
            if (byKey.containsKey(key)) {
                continue;
            }
            var id = hasText(intent.id()) ? intent.id() : "INT-%03d".formatted(generated++);
            var type = intent.type() != null ? intent.type() : inferType(statement);
            var priority = intent.priority() != null ? intent.priority() : Priority.REQUIRED;
            var provenance = intent.provenance() != null
                    ? intent.provenance()
                    : Provenance.of(EvidenceType.LLM_CANDIDATE, "candidate");
            byKey.put(key, new Intent(id, type, statement, priority, provenance));
        }
        return List.copyOf(byKey.values());
    }

    private List<Evidence> canonicalEvidence(List<Evidence> incoming) {
        var result = new ArrayList<Evidence>();
        int generated = 1;
        var seen = new LinkedHashSet<String>();
        for (var evidence : incoming) {
            var observation = collapse(evidence.observation());
            var key = ConflictDetector.normalize(observation);
            if (!seen.add(key)) {
                continue;
            }
            var id = hasText(evidence.id()) ? evidence.id() : "E-%03d".formatted(generated++);
            result.add(new Evidence(
                    id,
                    evidence.type() != null ? evidence.type() : EvidenceType.LLM_CANDIDATE,
                    hasText(evidence.source()) ? evidence.source() : "candidate",
                    observation
            ));
        }
        return List.copyOf(result);
    }

    private List<Invariant> deriveInvariants(List<Intent> intents) {
        var invariants = new ArrayList<Invariant>();
        var usedIds = new LinkedHashSet<String>();
        for (var intent : intents) {
            var derived = invariantsFor(intent);
            for (var invariant : derived) {
                var id = uniqueId(invariant.id(), usedIds);
                usedIds.add(id);
                invariants.add(new Invariant(id, invariant.description(), invariant.strategy(), invariant.relatedIntentIds()));
            }
        }
        return invariants;
    }

    private List<Invariant> invariantsFor(Intent intent) {
        var statement = ConflictDetector.normalize(intent.statement());
        var related = Set.of(intent.id());
        var derived = new ArrayList<Invariant>();

        if (statement.contains("refresh") && (statement.contains("rotat") || statement.contains("single-use"))) {
            derived.add(new Invariant(
                    "INV-001",
                    "A consumed refresh token cannot be reused.",
                    VerificationStrategy.JUNIT,
                    related
            ));
        }
        if (statement.contains("google")) {
            derived.add(new Invariant(
                    "INV-GOOGLE",
                    "Google OAuth integration tests continue to pass.",
                    VerificationStrategy.JUNIT,
                    related
            ));
        }
        if (statement.contains("apple")) {
            derived.add(new Invariant(
                    "INV-APPLE",
                    "Apple OAuth integration tests continue to pass.",
                    VerificationStrategy.JUNIT,
                    related
            ));
        }
        if (statement.contains("sessiontoken") || statement.contains("session token")) {
            derived.add(new Invariant(
                    "INV-JWT-SESSION-TOKEN",
                    "sessionToken remains present and compatible.",
                    VerificationStrategy.API_CONTRACT,
                    related
            ));
        }
        if (statement.contains("authorization code")) {
            derived.add(new Invariant(
                    "INV-AUTH-CODE",
                    "Existing authorization-code clients continue to authenticate.",
                    VerificationStrategy.JUNIT,
                    related
            ));
        }
        if (derived.isEmpty() && (intent.type() == IntentType.PRESERVATION || intent.type() == IntentType.CONSTRAINT)) {
            derived.add(new Invariant(
                    "INV-" + shortHash(statement),
                    "Preserve: " + intent.statement(),
                    VerificationStrategy.INTENT_DIFF,
                    related
            ));
        }
        if (derived.isEmpty() && intent.type() == IntentType.REQUIREMENT) {
            derived.add(new Invariant(
                    "INV-" + shortHash(statement),
                    intent.statement(),
                    VerificationStrategy.JUNIT,
                    related
            ));
        }
        return derived;
    }

    private List<SemanticRelation> evidenceRelations(List<Intent> intents, List<Evidence> evidence) {
        var relations = new ArrayList<SemanticRelation>();
        for (var intent : intents) {
            for (var item : evidence) {
                if (supports(intent, item)) {
                    relations.add(new SemanticRelation(
                            SemanticNode.intent(intent),
                            RelationType.DERIVED_FROM,
                            SemanticNode.evidence(item),
                            Provenance.of(item.type(), item.source())
                    ));
                }
            }
        }
        return relations;
    }

    private List<SemanticRelation> invariantRelations(List<Intent> intents, List<Invariant> invariants) {
        var byId = new LinkedHashMap<String, Intent>();
        intents.forEach(intent -> byId.put(intent.id(), intent));
        var relations = new ArrayList<SemanticRelation>();
        for (var invariant : invariants) {
            for (var intentId : invariant.relatedIntentIds()) {
                var intent = byId.get(intentId);
                if (intent == null) {
                    continue;
                }
                var type = intent.type() == IntentType.PRESERVATION ? RelationType.PRESERVES : RelationType.DERIVED_FROM;
                relations.add(new SemanticRelation(
                        SemanticNode.invariant(invariant),
                        type,
                        SemanticNode.intent(intent),
                        intent.provenance()
                ));
            }
        }
        return relations;
    }

    private List<SemanticRelation> conflictRelations(List<Intent> intents) {
        return conflictDetector.detect(intents).stream()
                .map(conflict -> new SemanticRelation(
                        SemanticNode.intent(conflict.left()),
                        RelationType.CONFLICTS_WITH,
                        SemanticNode.intent(conflict.right()),
                        Provenance.of(EvidenceType.LLM_CANDIDATE, "conflict-detector")
                ))
                .toList();
    }

    private static boolean supports(Intent intent, Evidence evidence) {
        var statement = ConflictDetector.normalize(intent.statement());
        var observation = ConflictDetector.normalize(evidence.observation());
        if (statement.contains("google") && observation.contains("google")) {
            return true;
        }
        if (statement.contains("apple") && observation.contains("apple")) {
            return true;
        }
        if ((statement.contains("sessiontoken") || statement.contains("session token"))
                && observation.contains("sessiontoken")) {
            return true;
        }
        if (statement.contains("authorization code") && observation.contains("authorization code")) {
            return true;
        }
        if (statement.contains("refresh") && observation.contains("refresh")) {
            return true;
        }
        if (statement.contains("login") && observation.contains("login")) {
            return true;
        }
        return !java.util.Collections.disjoint(
                ConflictDetector.significantTokens(statement),
                ConflictDetector.significantTokens(observation)
        );
    }

    private static IntentType inferType(String statement) {
        var normalized = ConflictDetector.normalize(statement);
        if (normalized.contains("preserve") || normalized.contains("must remain") || normalized.contains("must not change")) {
            return IntentType.PRESERVATION;
        }
        return IntentType.REQUIREMENT;
    }

    private static String collapse(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String uniqueId(String preferred, Set<String> used) {
        if (!used.contains(preferred)) {
            return preferred;
        }
        var suffix = 2;
        while (used.contains(preferred + "-" + suffix)) {
            suffix++;
        }
        return preferred + "-" + suffix;
    }

    private static String shortHash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 8).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
