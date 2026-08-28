package com.embabel.dif.dif;

import com.embabel.dif.domain.Evidence;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.MissingObligation;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Absence reasoning: obligations implied by intent that the repository does not yet satisfy.
 */
@Component
public class ObligationDeriver {

    public List<MissingObligation> derive(SemanticModel model, RepositoryAnalysis analysis) {
        var observations = analysis.evidence().stream()
                .map(Evidence::observation)
                .map(ObligationDeriver::normalize)
                .toList();
        var missing = new ArrayList<MissingObligation>();
        for (var intent : model.intents()) {
            for (var obligation : impliedObligations(intent)) {
                if (!present(obligation, observations)) {
                    missing.add(new MissingObligation(obligation, intent.id()));
                }
            }
        }
        return SemanticModel.canonicalObligations(missing);
    }

    private static List<String> impliedObligations(Intent intent) {
        var statement = normalize(intent.statement());
        if (statement.contains("refresh") && (statement.contains("rotat") || statement.contains("single-use"))) {
            return List.of(
                    "token family identifier",
                    "consumed-token state",
                    "replay detection",
                    "rotation integration test"
            );
        }
        return List.of();
    }

    private static boolean present(String obligation, List<String> observations) {
        var needle = normalize(obligation);
        return observations.stream().anyMatch(observation -> observation.contains(needle));
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
