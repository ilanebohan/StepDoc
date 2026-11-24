package com.exodius_software.stepdoc.core.model;

/**
 * Gherkin keyword family of a step, used to group and order steps, whatever the Gherkin
 * language of the annotation: {@code @Wenn} (German) is a {@link #WHEN}. A step annotation
 * whose keyword is not a known Gherkin keyword is reported as {@link #OTHER}.
 * The keyword as written stays available on {@link StepDefinition#keyword()}.
 */
public enum StepKind {
    GIVEN,
    WHEN,
    THEN,
    AND,
    BUT,
    OTHER
}
