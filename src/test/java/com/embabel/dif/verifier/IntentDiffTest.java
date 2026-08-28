package com.embabel.dif.verifier;

import com.embabel.dif.domain.SemanticProperty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDiffTest {

    @Test
    void passedFailsWhenARequiredPathIsRemoved() {
        var diff = new IntentDiff(
                List.of(),
                List.of(new SemanticProperty("provider.GOOGLE", "present")),
                List.of(
                        new SemanticProperty("provider.APPLE", "present"),
                        new SemanticProperty("jwt.claim.sessionToken", "present")
                )
        );

        assertThat(diff.passed()).isFalse();
        assertThat(diff.preserves(IntentDiff.DEFAULT_REQUIRED_PATHS)).isFalse();
        assertThat(diff.render()).contains("RESULT: FAIL").doesNotContain("RESULT: PASS");
    }

    @Test
    void passedWhenRequiredPathsAreUnchanged() {
        var diff = new IntentDiff(
                List.of(new SemanticProperty("refresh-token.rotates", "true")),
                List.of(new SemanticProperty("refresh-token.reusable", "true")),
                List.of(
                        new SemanticProperty("provider.GOOGLE", "present"),
                        new SemanticProperty("provider.APPLE", "present"),
                        new SemanticProperty("jwt.claim.sessionToken", "present")
                )
        );

        assertThat(diff.passed()).isTrue();
        assertThat(diff.preserves(IntentDiff.DEFAULT_REQUIRED_PATHS)).isTrue();
        assertThat(diff.render()).contains("RESULT: PASS");
    }

    @Test
    void snapshotsWithoutLoginKeysStillPass() {
        var diff = new IntentDiff(
                List.of(new SemanticProperty("api.orders", "added")),
                List.of(),
                List.of()
        );

        assertThat(diff.passed()).isTrue();
        assertThat(diff.preserves(IntentDiff.DEFAULT_REQUIRED_PATHS)).isFalse();
    }
}
