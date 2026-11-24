package fr.bohanilane.stepdoc.core.model;

/**
 * How Cucumber interprets a step pattern. Follows Cucumber's own rule: a pattern
 * starting with {@code ^}, ending with {@code $} or wrapped in {@code /} is a regular
 * expression, anything else is a Cucumber Expression.
 */
public enum ExpressionSyntax {
    CUCUMBER_EXPRESSION,
    REGULAR_EXPRESSION
}
