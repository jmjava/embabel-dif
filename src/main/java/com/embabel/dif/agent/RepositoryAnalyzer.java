package com.embabel.dif.agent;

import com.embabel.dif.domain.ChangeRequest;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.scenario.RefreshTokenScenario;
import org.springframework.stereotype.Component;

/**
 * Milestone 1 stub: returns canned repository evidence for the prototype scenario.
 * Phase 3 should replace this with real repository inspection.
 */
@Component
public class RepositoryAnalyzer {

    public RepositoryAnalysis analyze(ChangeRequest request, SemanticModel model) {
        if (RefreshTokenScenario.matches(request)) {
            return RefreshTokenScenario.repositoryAnalysis();
        }
        return new RepositoryAnalysis(java.util.List.of(), java.util.List.of());
    }
}
