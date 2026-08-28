package com.embabel.dif.canvas;

import com.embabel.dif.dif.ConflictDetector;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.dif.RuleBasedIntentFolder;
import com.embabel.dif.domain.IntentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasFolderTest {

    private final CanvasFolder folder = new CanvasFolder(
            new ReasonsCanvasParser(),
            new CanvasIntentMapper(),
            new RuleBasedIntentFolder(new ConflictDetector()),
            new ObligationDeriver()
    );

    @Test
    void sameCanvasFoldsToTheSameModel() throws Exception {
        var markdown = readExample("FEAT-001-order-status-api.md");
        assertThat(folder.fold(markdown)).isEqualTo(folder.fold(markdown));
    }

    @Test
    void orderStatusCanvasExposesOpenDocumentationObligation() throws Exception {
        var canvas = folder.parse(readExample("FEAT-001-order-status-api.md"));
        var model = folder.fold(canvas);

        assertThat(canvas.workId()).isEqualTo("FEAT-001-order-status-api");
        assertThat(model.hasBlockingConflicts()).isFalse();
        assertThat(model.intents())
                .anyMatch(intent -> intent.type() == IntentType.REQUIREMENT
                        && intent.statement().contains("GET /api/orders"))
                .anyMatch(intent -> intent.type() == IntentType.CONSTRAINT
                        && intent.statement().contains("Pagination"));
        assertThat(model.missingObligations())
                .anyMatch(obligation -> obligation.obligation().contains("T03")
                        && obligation.derivedFromIntent().equals("FEAT-001-order-status-api"));
        assertThat(model.missingObligations())
                .noneMatch(obligation -> obligation.obligation().contains("T01"));
    }

    @Test
    void paginationRequirementConflictsWithPaginationNonGoal() throws Exception {
        var model = folder.fold(readExample("FEAT-099-pagination-conflict.md"));

        assertThat(model.hasBlockingConflicts()).isTrue();
        assertThat(model.conflicts()).isNotEmpty();
    }

    private static String readExample(String name) throws IOException {
        return Files.readString(Path.of("examples/canvases", name), StandardCharsets.UTF_8);
    }
}
