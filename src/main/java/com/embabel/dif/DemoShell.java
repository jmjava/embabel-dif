package com.embabel.dif;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.dif.dif.IntentFolder;
import com.embabel.dif.domain.VerificationPlan;
import com.embabel.dif.scenario.RefreshTokenScenario;
import com.embabel.dif.verifier.SemanticVerifier;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
record DemoShell(
        AgentPlatform agentPlatform,
        IntentFolder intentFolder,
        SemanticVerifier semanticVerifier
) {

    @ShellMethod("Fold the prototype refresh-token request through Embabel")
    String fold() {
        var plan = AgentInvocation
                .create(agentPlatform, VerificationPlan.class)
                .invoke(new UserInput(RefreshTokenScenario.REQUEST_TEXT));
        return render(plan);
    }

    @ShellMethod("Fold a change request without going through the shell planner")
    String foldLocal() {
        var model = intentFolder.fold(RefreshTokenScenario.candidateIntent());
        return "intents=%d invariants=%d relations=%d conflicts=%d".formatted(
                model.intents().size(),
                model.invariants().size(),
                model.relations().size(),
                model.conflicts().size()
        );
    }

    @ShellMethod("Show the semantic diff for the refresh-token scenario")
    String intentDiff() {
        return semanticVerifier.diff(RefreshTokenScenario.before(), RefreshTokenScenario.desired()).render();
    }

    private static String render(VerificationPlan plan) {
        var model = plan.model();
        return """
                readyForImplementation=%s
                intents=%d
                invariants=%d
                relations=%d
                conflicts=%d
                missingObligations=%d
                rules=%d
                """.formatted(
                plan.readyForImplementation(),
                model.intents().size(),
                model.invariants().size(),
                model.relations().size(),
                model.conflicts().size(),
                plan.missingObligations().size(),
                plan.rules().size()
        ).trim();
    }
}
