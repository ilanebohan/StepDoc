package com.exodius_software.stepdoc.core.lint;

import com.exodius_software.stepdoc.core.util.EnumNames;

/** What {@link CatalogLinter} can check. Configured by id, e.g. {@code undocumented-step}. */
public enum LintRule {

    /** A step with no description, neither from Javadoc nor from {@code @StepDoc}. */
    UNDOCUMENTED_STEP,

    /** A step argument with no description, data tables and doc strings included. */
    UNDOCUMENTED_PARAMETER,

    /** Two step definitions with the same expression: Cucumber refuses to run them. */
    DUPLICATE_STEP,

    /** A step definition that no feature file uses. Skipped when feature files cannot be read. */
    UNUSED_STEP,

    /** A step of a feature file that no step definition implements. Skipped like {@link #UNUSED_STEP}. */
    UNDEFINED_STEP,

    /** A step of a feature file that several step definitions implement. Skipped like {@link #UNUSED_STEP}. */
    AMBIGUOUS_STEP,

    /**
     * Anything that could not be read or understood: unparsable file, parameter count
     * mismatch, expression that cannot be matched, and so on.
     */
    SOURCE_PROBLEM;

    public String id() {
        return EnumNames.idOf(this);
    }

    public static LintRule fromId(String id) {
        return EnumNames.fromName(LintRule.class, id, "rule");
    }
}
