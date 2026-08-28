package com.embabel.dif.dif;

import com.embabel.dif.domain.RelationType;
import com.embabel.dif.domain.SemanticNodeKind;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedIntentFolderTest {

    private final RuleBasedIntentFolder folder = new RuleBasedIntentFolder(new ConflictDetector());

    @Test
    void sameCandidateProducesIdenticalCanonicalModel() {
        var first = folder.fold(RefreshTokenScenario.candidateIntent());
        var second = folder.fold(RefreshTokenScenario.candidateIntent());

        assertThat(first).isEqualTo(second);
        assertThat(first.intents()).hasSize(5);
        assertThat(first.invariants()).isNotEmpty();
        assertThat(first.hasBlockingConflicts()).isFalse();
    }

    @Test
    void refreshTokenScenarioDerivesRotationAndPreservationInvariants() {
        var model = folder.fold(RefreshTokenScenario.candidateIntent());
        var descriptions = model.invariants().stream().map(invariant -> invariant.description()).toList();

        assertThat(descriptions).anyMatch(text -> text.contains("consumed refresh token"));
        assertThat(descriptions).anyMatch(text -> text.contains("Google OAuth"));
        assertThat(descriptions).anyMatch(text -> text.contains("Apple OAuth"));
        assertThat(descriptions).anyMatch(text -> text.contains("sessionToken"));
        assertThat(descriptions).anyMatch(text -> text.contains("authorization-code"));
    }

    @Test
    void invariantsTraceBackToIntentAndEvidence() {
        var model = folder.fold(RefreshTokenScenario.candidateIntent());

        assertThat(model.relations())
                .anyMatch(relation -> relation.type() == RelationType.DERIVED_FROM
                        && relation.from().kind() == SemanticNodeKind.INVARIANT)
                .anyMatch(relation -> relation.type() == RelationType.DERIVED_FROM
                        && relation.to().kind() == SemanticNodeKind.EVIDENCE)
                .anyMatch(relation -> relation.type() == RelationType.PRESERVES);
    }
}
