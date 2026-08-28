package com.embabel.dif.verifier;

import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedSemanticVerifierTest {

    private final RuleBasedSemanticVerifier verifier = new RuleBasedSemanticVerifier();

    @Test
    void semanticDiffMatchesPrototypeExample() {
        var diff = verifier.diff(RefreshTokenScenario.before(), RefreshTokenScenario.desired());

        assertThat(diff.added()).extracting(property -> property.path())
                .contains("refresh-token.rotates");
        assertThat(diff.removed()).extracting(property -> property.path())
                .contains("refresh-token.reusable");
        assertThat(diff.unchanged()).extracting(property -> property.path())
                .containsExactly("jwt.claim.sessionToken", "provider.APPLE", "provider.GOOGLE");
        assertThat(diff.preserves(RuleBasedSemanticVerifier.REQUIRED_UNCHANGED)).isTrue();
        assertThat(diff.render()).contains("SEMANTIC DIFF", "+ refresh-token.rotates", "RESULT: PASS");
    }
}
