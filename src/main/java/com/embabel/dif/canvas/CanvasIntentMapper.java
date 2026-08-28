package com.embabel.dif.canvas;

import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.Evidence;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.Priority;
import com.embabel.dif.domain.Provenance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Maps an accepted canvas into candidate facts. No LLM.
 * Section headings decide type (goal vs safety), not suggested wording.
 * // goal/safety: intent-lang
 */
@Component
public class CanvasIntentMapper {

    public CandidateIntent toCandidate(ReasonsCanvas canvas) {
        var intents = new ArrayList<Intent>();
        var evidence = new ArrayList<Evidence>();
        int intentSeq = 1;
        int evidenceSeq = 1;

        for (var criterion : canvas.acceptanceCriteria()) {
            intents.add(intent("INT-%03d".formatted(intentSeq++), IntentType.REQUIREMENT, criterion,
                    Provenance.of(EvidenceType.ISSUE, canvas.workId() + ":R")));
        }
        for (var nonGoal : canvas.nonGoals()) {
            intents.add(intent("INT-%03d".formatted(intentSeq++), IntentType.CONSTRAINT, "Non-goal: " + nonGoal,
                    Provenance.of(EvidenceType.ISSUE, canvas.workId() + ":R-nongoal")));
        }
        for (var safeguard : canvas.safeguards()) {
            intents.add(intent("INT-%03d".formatted(intentSeq++), IntentType.PRESERVATION, safeguard,
                    Provenance.of(EvidenceType.ADR, canvas.workId() + ":S")));
        }
        for (var norm : canvas.norms()) {
            intents.add(intent("INT-%03d".formatted(intentSeq++), IntentType.PRESERVATION, norm,
                    Provenance.of(EvidenceType.ADR, canvas.workId() + ":N")));
        }
        for (var assumption : canvas.assumptions()) {
            evidence.add(new Evidence(
                    "E-%03d".formatted(evidenceSeq++),
                    EvidenceType.DOCUMENTATION,
                    canvas.workId() + ":assumption",
                    assumption
            ));
        }
        for (var entity : canvas.entities()) {
            evidence.add(new Evidence(
                    "E-%03d".formatted(evidenceSeq++),
                    EvidenceType.SOURCE_CODE,
                    canvas.workId() + ":entity",
                    "Domain entity " + entity
            ));
        }
        for (var file : canvas.filesLikelyAffected()) {
            evidence.add(new Evidence(
                    "E-%03d".formatted(evidenceSeq++),
                    EvidenceType.SOURCE_CODE,
                    file,
                    "File likely affected: " + file
            ));
        }
        return new CandidateIntent(intents, evidence);
    }

    private static Intent intent(String id, IntentType type, String statement, Provenance provenance) {
        return new Intent(id, type, statement, Priority.REQUIRED, provenance);
    }
}
