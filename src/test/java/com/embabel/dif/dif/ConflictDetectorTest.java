package com.embabel.dif.dif;

import com.embabel.dif.domain.ConflictReason;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.Priority;
import com.embabel.dif.domain.Provenance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConflictDetectorTest {

    private final ConflictDetector detector = new ConflictDetector();

    @Test
    void detectsSingleUseVersusIndefiniteReuse() {
        var source = Provenance.of(EvidenceType.USER_STATEMENT, "test");
        var conflicts = detector.detect(List.of(
                new Intent("INT-100", IntentType.REQUIREMENT, "Refresh tokens must be single-use.", Priority.REQUIRED, source),
                new Intent("INT-101", IntentType.REQUIREMENT, "Existing clients must be able to reuse the same refresh token indefinitely.", Priority.REQUIRED, source)
        ));

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.getFirst().reason()).isEqualTo(ConflictReason.MUTUALLY_EXCLUSIVE);
        assertThat(conflicts.getFirst().blocking()).isTrue();
    }
}
