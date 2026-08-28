package com.embabel.dif.live;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.dif.canvas.CanvasFolder;
import com.embabel.dif.dif.VerificationPlanner;
import com.embabel.dif.domain.VerificationPlan;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live Embabel: boots the Spring agent platform. Does not start {@code sdlc.sh next}.
 * Fixture path needs no LLM. Canvas planning uses already-folded facts.
 */
@SpringBootTest
@TestPropertySource(properties = "embabel.agent.shell.interactive.enabled=false")
@EnabledIfEnvironmentVariable(named = "DIF_LIVE_EMBABEL", matches = "1")
class EmbabelLivePlatformTest {

    @Autowired
    AgentPlatform agentPlatform;

    @Autowired
    CanvasFolder canvasFolder;

    @Autowired
    VerificationPlanner planner;

    @Test
    void liveGoapFoldsRefreshTokenThroughTheAgentPlatform() {
        var plan = AgentInvocation
                .create(agentPlatform, VerificationPlan.class)
                .invoke(RefreshTokenScenario.request());

        assertThat(plan.readyForImplementation()).isTrue();
        assertThat(plan.rules()).isNotEmpty();
        assertThat(plan.missingObligations())
                .anyMatch(obligation -> obligation.obligation().equals("rotation integration test"));
    }

    @Test
    void livePlannerReadsAFoldedOrchCanvasWithoutReparsingMarkdown() throws Exception {
        var markdown = Files.readString(Path.of("examples/canvases/FEAT-001-order-status-api.md"));
        var model = canvasFolder.fold(markdown);
        var plan = planner.plan(model);

        assertThat(plan.model().intents()).isEqualTo(model.intents());
        assertThat(plan.readyForImplementation()).isTrue();
        assertThat(plan.missingObligations())
                .anyMatch(obligation -> obligation.obligation().contains("T03"));
    }
}
