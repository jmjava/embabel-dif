package com.embabel.dif.memory;

import com.embabel.dif.domain.SemanticModel;

public final class SemanticModelRenderer {

    private SemanticModelRenderer() {
    }

    public static String render(SemanticModel model) {
        var text = new StringBuilder();
        text.append("readyForImplementation=").append(!model.hasBlockingConflicts()).append('\n');
        text.append("intents:\n");
        model.intents().forEach(intent ->
                text.append("  - ").append(intent.id()).append(" [").append(intent.type()).append("] ")
                        .append(intent.statement()).append('\n'));
        text.append("invariants:\n");
        model.invariants().forEach(invariant ->
                text.append("  - ").append(invariant.id()).append(": ").append(invariant.description()).append('\n'));
        text.append("conflicts:\n");
        if (model.conflicts().isEmpty()) {
            text.append("  - none\n");
        }
        model.conflicts().forEach(conflict ->
                text.append("  - ").append(conflict.left().id()).append(" CONFLICTS_WITH ")
                        .append(conflict.right().id()).append(" (").append(conflict.explanation()).append(")\n"));
        text.append("missingObligations:\n");
        if (model.missingObligations().isEmpty()) {
            text.append("  - none\n");
        }
        model.missingObligations().forEach(obligation ->
                text.append("  - ").append(obligation.derivedFromIntent()).append(": ")
                        .append(obligation.obligation()).append('\n'));
        return text.toString();
    }
}
