package com.embabel.dif.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.dif.dif.IntentFolder;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.ChangeRequest;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.VerificationPlan;
import com.embabel.dif.verifier.VerificationRule;

import java.util.List;

/**
 * Milestone 1 agent: probabilistic interpretation, deterministic fold, GOAP to a verification plan.
 */
@Agent(description = "Fold change-request intent into a typed semantic model and plan deterministic verification")
public class DifEmbabelAgent {

    private final List<IntentInterpreter> interpreters;
    private final IntentFolder intentFolder;
    private final RepositoryAnalyzer repositoryAnalyzer;
    private final ObligationDeriver obligationDeriver;

    public DifEmbabelAgent(
            List<IntentInterpreter> interpreters,
            IntentFolder intentFolder,
            RepositoryAnalyzer repositoryAnalyzer,
            ObligationDeriver obligationDeriver
    ) {
        this.interpreters = interpreters;
        this.intentFolder = intentFolder;
        this.repositoryAnalyzer = repositoryAnalyzer;
        this.obligationDeriver = obligationDeriver;
    }

    @Action
    public ChangeRequest captureRequest(UserInput userInput) {
        return new ChangeRequest(userInput.getContent());
    }

    @Action
    public CandidateIntent interpretIntent(ChangeRequest request, Ai ai) {
        return interpreters.stream()
                .filter(interpreter -> interpreter.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No intent interpreter available"))
                .interpret(request, ai);
    }

    @Action
    public SemanticModel foldIntent(CandidateIntent candidate) {
        return intentFolder.fold(candidate);
    }

    @Action
    public RepositoryAnalysis analyzeRepository(ChangeRequest request, SemanticModel semanticModel) {
        return repositoryAnalyzer.analyze(request, semanticModel);
    }

    @Condition(name = "noBlockingIntentConflicts")
    public boolean noBlockingIntentConflicts(SemanticModel semanticModel) {
        return !semanticModel.hasBlockingConflicts();
    }

    @AchievesGoal(
            description = "Intent has been folded into typed semantic state and a verification plan is ready",
            export = @Export(remote = true, name = "planSemanticVerification")
    )
    @Action(pre = {"noBlockingIntentConflicts"})
    public VerificationPlan planVerification(SemanticModel semanticModel, RepositoryAnalysis analysis) {
        var missing = obligationDeriver.derive(semanticModel, analysis);
        var model = semanticModel.withMissingObligations(missing);
        var rules = model.invariants().stream()
                .map(invariant -> new VerificationRule(
                        invariant.id(),
                        invariant.strategy(),
                        invariant.description()
                ))
                .toList();
        return new VerificationPlan(model, analysis, rules, missing);
    }
}
