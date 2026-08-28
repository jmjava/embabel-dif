package com.embabel.dif.agent;

import com.embabel.dif.dif.ConflictDetector;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.dif.RuleBasedIntentFolder;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DifEmbabelAgentTest {

    @Test
    void milestoneOneTypesChainWithoutAnLlm() {
        var folder = new RuleBasedIntentFolder(new ConflictDetector());
        var agent = new DifEmbabelAgent(
                List.of(new FixtureIntentInterpreter()),
                folder,
                new RepositoryAnalyzer(),
                new ObligationDeriver()
        );

        var candidate = agent.interpretIntent(RefreshTokenScenario.request(), null);
        var model = agent.foldIntent(candidate);
        var analysis = agent.analyzeRepository(RefreshTokenScenario.request(), model);
        var plan = agent.planVerification(model, analysis);

        assertThat(candidate.intents()).hasSize(5);
        assertThat(model).isEqualTo(folder.fold(candidate));
        assertThat(plan.readyForImplementation()).isTrue();
        assertThat(plan.rules()).isNotEmpty();
        assertThat(plan.missingObligations())
                .anyMatch(obligation -> obligation.obligation().equals("rotation integration test"));
    }
}
