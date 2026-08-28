package com.embabel.dif.dif;

import com.embabel.dif.domain.MissingObligation;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.VerificationPlan;
import com.embabel.dif.verifier.VerificationRule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builds a {@link VerificationPlan} from an already-folded model.
 * Embabel may call this; it does not re-interpret markdown.
 * // Plan over typed facts after they exist: Embabel GOAP
 */
@Component
public class VerificationPlanner {

    private final ObligationDeriver obligationDeriver;

    public VerificationPlanner(ObligationDeriver obligationDeriver) {
        this.obligationDeriver = obligationDeriver;
    }

    public VerificationPlan plan(SemanticModel model, RepositoryAnalysis analysis) {
        var merged = new LinkedHashMap<String, MissingObligation>();
        for (var obligation : model.missingObligations()) {
            merged.put(key(obligation), obligation);
        }
        for (var obligation : obligationDeriver.derive(model, analysis)) {
            merged.putIfAbsent(key(obligation), obligation);
        }
        var canonical = SemanticModel.canonicalObligations(List.copyOf(merged.values()));
        var withMissing = model.withMissingObligations(canonical);
        var rules = withMissing.invariants().stream()
                .map(invariant -> new VerificationRule(
                        invariant.id(),
                        invariant.strategy(),
                        invariant.description()
                ))
                .toList();
        return new VerificationPlan(withMissing, analysis, rules, canonical);
    }

    public VerificationPlan plan(SemanticModel model) {
        return plan(model, new RepositoryAnalysis(List.of(), List.of()));
    }

    private static String key(MissingObligation obligation) {
        return obligation.derivedFromIntent() + "\0" + obligation.obligation();
    }
}
