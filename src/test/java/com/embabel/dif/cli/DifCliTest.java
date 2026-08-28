package com.embabel.dif.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DifCliTest {

    @TempDir
    Path tempDir;

    @Test
    void foldWritesProjectionAndExitsZeroWhenCanvasIsConsistent() throws Exception {
        var code = DifCli.run(new String[]{
                "fold",
                "--canvas",
                "examples/canvases/FEAT-001-order-status-api.md",
                "--out",
                tempDir.toString()
        });

        assertThat(code).isZero();
        assertThat(Files.readString(tempDir.resolve("FEAT-001-order-status-api.txt")))
                .contains("T03 - Document API behavior")
                .contains("readyForImplementation=true");
        assertThat(tempDir.resolve("FEAT-001-order-status-api.json")).exists();
    }

    @Test
    void foldExitsOneWhenCanvasHasBlockingConflicts() throws Exception {
        var code = DifCli.run(new String[]{
                "fold",
                "--canvas",
                "examples/canvases/FEAT-099-pagination-conflict.md",
                "--out",
                tempDir.toString()
        });

        assertThat(code).isEqualTo(1);
        assertThat(Files.readString(tempDir.resolve("FEAT-099-pagination-conflict.txt")))
                .contains("CONFLICTS_WITH");
    }
}
