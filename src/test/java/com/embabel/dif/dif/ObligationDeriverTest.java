package com.embabel.dif.dif;

import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObligationDeriverTest {

    @Test
    void rotationIntegrationTestIsMissingFromStubRepository() {
        var folder = new RuleBasedIntentFolder(new ConflictDetector());
        var model = folder.fold(RefreshTokenScenario.candidateIntent());
        var missing = new ObligationDeriver().derive(model, RefreshTokenScenario.repositoryAnalysis());

        assertThat(missing)
                .anyMatch(obligation -> obligation.obligation().equals("rotation integration test")
                        && obligation.derivedFromIntent().equals("INT-001"));
        assertThat(missing)
                .noneMatch(obligation -> obligation.obligation().equals("token family identifier"));
    }
}
