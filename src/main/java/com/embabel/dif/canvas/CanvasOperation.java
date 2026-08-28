package com.embabel.dif.canvas;

import java.util.Locale;

public record CanvasOperation(
        String id,
        String name,
        String status,
        String description
) {
    public boolean complete() {
        var value = status == null ? "" : status.toLowerCase(Locale.ROOT);
        return value.contains("complete") || value.contains("done");
    }
}
