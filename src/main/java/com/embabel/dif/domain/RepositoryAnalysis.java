package com.embabel.dif.domain;

import java.util.List;

public record RepositoryAnalysis(
        List<String> affectedFiles,
        List<Evidence> evidence
) {
    public RepositoryAnalysis {
        affectedFiles = List.copyOf(affectedFiles);
        evidence = List.copyOf(evidence);
    }
}
