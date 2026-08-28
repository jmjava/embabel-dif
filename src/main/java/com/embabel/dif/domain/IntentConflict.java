package com.embabel.dif.domain;

public record IntentConflict(
        Intent left,
        Intent right,
        ConflictReason reason,
        String explanation
) {
    public boolean blocking() {
        return reason == ConflictReason.MUTUALLY_EXCLUSIVE
                && (left.priority() == Priority.REQUIRED || right.priority() == Priority.REQUIRED);
    }
}
