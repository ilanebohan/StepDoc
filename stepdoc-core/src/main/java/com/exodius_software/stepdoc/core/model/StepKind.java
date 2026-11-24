package com.exodius_software.stepdoc.core.model;

/**
 * Gherkin keyword family of a step, used to group and order steps.
 * Keywords from localized annotation packages are reported as {@link #OTHER};
 * the raw keyword stays available on {@link StepDefinition#keyword()}.
 */
public enum StepKind {
    GIVEN,
    WHEN,
    THEN,
    AND,
    BUT,
    OTHER
}
