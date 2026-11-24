package fr.bohanilane.stepdoc.core.lint;

import fr.bohanilane.stepdoc.core.util.EnumNames;

/** What {@link CatalogLinter} can check. Configured by id, e.g. {@code undocumented-step}. */
public enum LintRule {

    /** A step with no description, neither from Javadoc nor from {@code @StepDoc}. */
    UNDOCUMENTED_STEP,

    /** A step argument with no description, data tables and doc strings included. */
    UNDOCUMENTED_PARAMETER,

    /** Two step definitions with the same expression: Cucumber refuses to run them. */
    DUPLICATE_STEP,

    /** Anything the parser reported: unreadable file, parameter count mismatch, and so on. */
    SOURCE_PROBLEM;

    public String id() {
        return EnumNames.idOf(this);
    }

    public static LintRule fromId(String id) {
        return EnumNames.fromName(LintRule.class, id, "rule");
    }
}
