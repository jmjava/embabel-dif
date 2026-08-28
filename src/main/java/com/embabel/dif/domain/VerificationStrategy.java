package com.embabel.dif.domain;

public enum VerificationStrategy {
    JUNIT,
    ARCHUNIT,
    COMPILER,
    SCHEMA,
    API_CONTRACT,
    AST,
    STATIC_ANALYSIS,
    REPOSITORY_QUERY,
    INTENT_DIFF,
    MANUAL
}
