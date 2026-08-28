package com.embabel.dif.domain;

import java.util.List;

/**
 * Phase 3 stub: LLM-generated implementation constrained by the semantic model.
 */
public record ProposedChange(List<FileChange> files) {
    public ProposedChange {
        files = List.copyOf(files);
    }
}
