package com.embabel.dif.dif;

import com.embabel.dif.FoldWiring;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 6 — T03-style gaps on a canvas that never mentions refresh.
 */
class WebhookObligationTest {

    @Test
    void webhookCanvasExposesRetryObligationsWithoutRefreshWording() throws Exception {
        var markdown = Files.readString(
                Path.of("examples/canvases/FEAT-020-webhook-retry.md"),
                StandardCharsets.UTF_8
        );
        var model = FoldWiring.canvasFolder().fold(markdown);
        var texts = model.missingObligations().stream().map(obligation -> obligation.obligation()).toList();

        assertThat(markdown.toLowerCase()).doesNotContain("refresh");
        assertThat(texts).anyMatch(text -> text.contains("T03"));
        assertThat(texts).anyMatch(text -> text.contains("idempotency key"));
        assertThat(texts).anyMatch(text -> text.contains("retry integration test"));
        assertThat(texts).noneMatch(text -> text.toLowerCase().contains("refresh"));
        assertThat(model.hasBlockingConflicts()).isFalse();
    }
}
