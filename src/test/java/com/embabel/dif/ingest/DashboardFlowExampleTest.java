package com.embabel.dif.ingest;

import com.embabel.dif.FoldWiring;
import com.embabel.dif.cli.GateReport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example canvases for {@code scripts/dif-dashboard-e2e.sh}: same Work ID,
 * contract change, gate line the Dashboard reads.
 */
class DashboardFlowExampleTest {

    @Test
    void blockedCanvasFailsClosedThenReadyCanvasClearsTheGate() throws Exception {
        var folder = FoldWiring.canvasFolder();
        var blocked = folder.fold(Files.readString(
                Path.of("examples/dashboard-flow/FEAT-DASH-flow.blocked.md"),
                StandardCharsets.UTF_8
        ));
        var ready = folder.fold(Files.readString(
                Path.of("examples/dashboard-flow/FEAT-DASH-flow.ready.md"),
                StandardCharsets.UTF_8
        ));

        var blockedGate = GateReport.from("FEAT-DASH-flow", blocked);
        var readyGate = GateReport.from("FEAT-DASH-flow", ready);

        assertThat(blocked.hasBlockingConflicts()).isTrue();
        assertThat(blockedGate.oneLine()).isEqualTo(
                "dif=blocked workId=FEAT-DASH-flow readyForImplementation=false conflicts=1"
        );
        assertThat(ready.hasBlockingConflicts()).isFalse();
        assertThat(readyGate.oneLine()).isEqualTo(
                "dif=ready workId=FEAT-DASH-flow readyForImplementation=true"
        );
    }
}
