package com.embabel.dif.dif;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.domain.VerificationStrategy;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 7 — DTO / test names are syntax. Preservation invariants stay put.
 */
class SyntaxVarianceTest {

    @Test
    void renamingDtoAndTestFilesDoesNotChangePreservationInvariants() throws Exception {
        var folder = FoldWiring.canvasFolder();
        var original = folder.fold(read("FEAT-070-dto-rename-a.md"));
        var renamed = folder.fold(read("FEAT-070-dto-rename-b.md"));

        var originalPreserve = original.invariants().stream()
                .filter(invariant -> invariant.strategy() == VerificationStrategy.INTENT_DIFF
                        || invariant.description().toLowerCase().contains("auth")
                        || invariant.description().toLowerCase().contains("unrelated"))
                .map(invariant -> invariant.description())
                .toList();
        var renamedPreserve = renamed.invariants().stream()
                .filter(invariant -> invariant.strategy() == VerificationStrategy.INTENT_DIFF
                        || invariant.description().toLowerCase().contains("auth")
                        || invariant.description().toLowerCase().contains("unrelated"))
                .map(invariant -> invariant.description())
                .toList();

        assertThat(originalPreserve).isEqualTo(renamedPreserve);
        assertThat(originalPreserve).isNotEmpty();
        assertThat(original.invariants())
                .noneMatch(invariant -> invariant.description().contains("OrderStatusDto"));
        assertThat(renamed.invariants())
                .noneMatch(invariant -> invariant.description().contains("OrderLookupResponse"));
    }

    private static String read(String name) throws Exception {
        return Files.readString(Path.of("examples/canvases", name), StandardCharsets.UTF_8);
    }
}
