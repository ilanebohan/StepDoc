package fr.bohanilane.stepdoc.core.lint;

import java.util.Objects;

import fr.bohanilane.stepdoc.core.model.SourceLocation;

/** A rule broken at a given place. */
public record Violation(LintRule rule, SourceLocation location, String message) {

    public Violation {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }

    /** {@code [undocumented-step] steps/CartSteps.java:12 Step "I add a book" has no description} */
    @Override
    public String toString() {
        return "[" + rule.id() + "] " + location + " " + message;
    }
}
