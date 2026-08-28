package com.embabel.dif.dif;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.Priority;
import com.embabel.dif.domain.Provenance;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormalConflictBackendTest {

    @Test
    void tokenOverlapMissesExactlyOnceVersusDuplicates() {
        var once = ConflictDetector.significantTokens("Exactly-once delivery is required");
        var dups = ConflictDetector.significantTokens("Duplicates are acceptable");

        assertThat(once).doesNotContainAnyElementsOf(dups);
        assertThat(new FormalConflictBackend().detect(
                intent("INT-A", "Exactly-once delivery is required"),
                intent("INT-B", "Duplicates are acceptable")
        )).isPresent();
    }

    @Test
    void canvasFiresFormalVacuityAndEmitsAlloy() throws Exception {
        var markdown = Files.readString(
                Path.of("examples/canvases/FEAT-080-exactly-once-vs-duplicates.md"),
                StandardCharsets.UTF_8
        );
        var model = FoldWiring.canvasFolder().fold(markdown);

        assertThat(model.hasBlockingConflicts()).isTrue();
        assertThat(model.conflicts().getFirst().explanation())
                .contains("Formal vacuity")
                .contains("Exactly-once delivery is required")
                .contains("Duplicates are acceptable");
        assertThat(AlloyModelEmitter.emit("FEAT-080", model))
                .contains("fact Vacuity")
                .contains("[ExactlyOnce]")
                .contains("[DuplicatesOk]");
    }

    @Test
    void tokenRulesStillCatchRefreshWithoutFormalBackend() {
        var source = Provenance.of(EvidenceType.USER_STATEMENT, "test");
        var conflicts = new ConflictDetector().detect(List.of(
                new Intent("INT-100", IntentType.REQUIREMENT, "Refresh tokens must be single-use.", Priority.REQUIRED, source),
                new Intent("INT-101", IntentType.REQUIREMENT, "Existing clients must be able to reuse the same refresh token indefinitely.", Priority.REQUIRED, source)
        ));

        assertThat(conflicts.getFirst().explanation())
                .contains("Refresh-token")
                .contains("single-use")
                .contains("indefinitely");
    }

    private static Intent intent(String id, String statement) {
        return new Intent(
                id,
                IntentType.REQUIREMENT,
                statement,
                Priority.REQUIRED,
                Provenance.of(EvidenceType.USER_STATEMENT, "formal")
        );
    }
}
