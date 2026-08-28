package com.embabel.dif.verifier;

import com.embabel.dif.FoldWiring;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SafeguardPathsTest {

    @Test
    void slugsCanvasSafeguardsAndIgnoresNorms() throws Exception {
        var markdown = Files.readString(
                Path.of("examples/canvases/FEAT-001-order-status-api.md"),
                StandardCharsets.UTF_8
        );
        var model = FoldWiring.canvasFolder().fold(markdown);

        assertThat(SafeguardPaths.fromCanvasSafeguards(model)).containsExactlyInAnyOrder(
                "safeguard.auth-behavior",
                "safeguard.unrelated-api-endpoints",
                "safeguard.dependencies-without-justification"
        );
        assertThat(SafeguardPaths.fromCanvasSafeguards(model))
                .noneMatch(path -> path.contains("constructor") || path.contains("package"));
    }
}
