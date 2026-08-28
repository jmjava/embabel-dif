package com.embabel.dif.canvas;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.domain.IntentType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 4 — headings decide type. Moving a bullet is enough to change
 * IntentType and, when both sides remain, to introduce a conflict.
 */
class CanvasClassificationTest {

    private final CanvasFolder folder = FoldWiring.canvasFolder();

    @Test
    void paginationUnderAcceptanceIsARequirement() throws Exception {
        var model = folder.fold(Files.readString(
                Path.of("examples/canvases/FEAT-021-paginate-required.md"),
                StandardCharsets.UTF_8
        ));

        assertThat(model.hasBlockingConflicts()).isFalse();
        assertThat(model.intents())
                .anyMatch(intent -> intent.type() == IntentType.REQUIREMENT
                        && intent.statement().contains("paginated"));
        assertThat(model.intents())
                .noneMatch(intent -> intent.type() == IntentType.CONSTRAINT
                        && intent.statement().toLowerCase().contains("paginat"));
    }

    @Test
    void movingPaginationToNonGoalsChangesTypeAndCanConflict() throws Exception {
        var onlyNonGoal = """
                # REASONS Canvas: FEAT-021-paginate-required - Paginated list only
                
                ## Metadata
                - Work ID: FEAT-021-paginate-required
                
                ## R - Requirements
                
                ### Acceptance Criteria
                
                - [ ] List results are searchable
                
                ### Non-Goals
                
                - Results must be paginated
                """;
        var bothSides = """
                # REASONS Canvas: FEAT-021-paginate-required - Paginated list only
                
                ## Metadata
                - Work ID: FEAT-021-paginate-required
                
                ## R - Requirements
                
                ### Acceptance Criteria
                
                - [ ] Results must be paginated
                
                ### Non-Goals
                
                - Results must be paginated
                """;

        var moved = folder.fold(onlyNonGoal);
        var conflicted = folder.fold(bothSides);

        assertThat(moved.intents())
                .anyMatch(intent -> intent.type() == IntentType.CONSTRAINT
                        && intent.statement().contains("paginated"));
        assertThat(moved.intents())
                .noneMatch(intent -> intent.type() == IntentType.REQUIREMENT
                        && intent.statement().contains("paginated"));
        assertThat(conflicted.hasBlockingConflicts()).isTrue();
        assertThat(conflicted.conflicts().getFirst().explanation())
                .contains("Results must be paginated");
    }
}
