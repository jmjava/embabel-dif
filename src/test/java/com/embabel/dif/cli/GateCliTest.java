package com.embabel.dif.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GateCliTest {

    @TempDir
    Path tempDir;

    @Test
    void architectFailsClosedFromProjectionAlone() throws Exception {
        assertThat(DifCli.run(new String[]{
                "fold",
                "--canvas",
                "examples/canvases/FEAT-099-pagination-conflict.md",
                "--out",
                tempDir.toString()
        })).isEqualTo(1);

        var code = DifCli.run(new String[]{
                "architect",
                "--projection",
                tempDir.resolve("FEAT-099-pagination-conflict.json").toString()
        });

        assertThat(code).isEqualTo(1);
    }

    @Test
    void reviewFailsWhenRequiredLoginPropertyIsRemoved() throws Exception {
        var original = System.out;
        var buffer = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(buffer));
        int code;
        try {
            code = DifCli.run(new String[]{
                    "review",
                    "--before",
                    "examples/snapshots/login-before.json",
                    "--after",
                    "examples/snapshots/login-auth-broken.json"
            });
        } finally {
            System.setOut(original);
        }

        assertThat(code).isEqualTo(1);
        assertThat(buffer.toString()).contains("RESULT: FAIL").doesNotContain("RESULT: PASS");
    }

    @Test
    void foldWritesGateJsonAScriptCanTrust() throws Exception {
        assertThat(DifCli.run(new String[]{
                "fold",
                "--canvas",
                "examples/canvases/FEAT-001-order-status-api.md",
                "--out",
                tempDir.toString()
        })).isZero();

        var gate = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(tempDir.resolve("FEAT-001-order-status-api.gate.json").toFile(), GateReport.class);
        assertThat(gate.workId()).isEqualTo("FEAT-001-order-status-api");
        assertThat(gate.readyForImplementation()).isTrue();
        assertThat(gate.blockingConflicts()).isEmpty();
        assertThat(gate.missingObligations()).anyMatch(text -> text.contains("T03"));
    }

    @Test
    void reviewPassesWhenRequiredLoginPropertiesArePreserved() throws Exception {
        var code = DifCli.run(new String[]{
                "review",
                "--before",
                "examples/snapshots/login-before.json",
                "--after",
                "examples/snapshots/login-desired.json"
        });

        assertThat(code).isZero();
    }

    @Test
    void planWritesVerificationPlanFromFoldedCanvas() throws Exception {
        var code = DifCli.run(new String[]{
                "plan",
                "--canvas",
                "examples/canvases/FEAT-001-order-status-api.md",
                "--out",
                tempDir.toString()
        });

        assertThat(code).isZero();
        assertThat(Files.readString(tempDir.resolve("FEAT-001-order-status-api.plan.json")))
                .contains("\"readyForImplementation\" : true")
                .contains("T03");
    }

    @Test
    void guideWritesDecisionAndPitfallNodes() throws Exception {
        var code = DifCli.run(new String[]{
                "guide",
                "--canvas",
                "examples/canvases/FEAT-099-pagination-conflict.md",
                "--out",
                tempDir.toString()
        });

        assertThat(code).isZero();
        var jsonl = Files.readString(tempDir.resolve("FEAT-099-pagination-conflict.guide.jsonl"));
        assertThat(jsonl).contains("\"kind\":\"Pitfall\"").contains("pagination");
    }

    @Test
    void foldCanEmitAlloySketch() throws Exception {
        var code = DifCli.run(new String[]{
                "fold",
                "--canvas",
                "examples/canvases/FEAT-080-exactly-once-vs-duplicates.md",
                "--out",
                tempDir.toString(),
                "--alloy"
        });

        assertThat(code).isEqualTo(1);
        assertThat(Files.readString(tempDir.resolve("FEAT-080-exactly-once-vs-duplicates.als")))
                .contains("fact Vacuity");
    }
}
