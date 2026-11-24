package fr.bohanilane.stepdoc.core.model;

import java.util.List;
import java.util.Objects;

/**
 * A documented step: one step annotation on one method. A method carrying several
 * step annotations produces several definitions sharing the same documentation.
 *
 * @param keyword        annotation simple name as written ({@code Given}, {@code When}...)
 * @param kind           keyword family, {@link StepKind#OTHER} when not recognised
 * @param expression     step pattern exactly as Cucumber receives it
 * @param syntax         how Cucumber interprets {@code expression}
 * @param description    what the step does, empty when undocumented
 * @param parameters     method arguments, in declaration order
 * @param declaringClass fully qualified name of the class declaring the method
 * @param methodName     Java method name
 * @param location       position of the step annotation
 */
public record StepDefinition(
        String keyword,
        StepKind kind,
        String expression,
        ExpressionSyntax syntax,
        String description,
        List<StepParameter> parameters,
        String declaringClass,
        String methodName,
        SourceLocation location) {

    public StepDefinition {
        Objects.requireNonNull(keyword, "keyword");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(syntax, "syntax");
        Objects.requireNonNull(description, "description");
        parameters = List.copyOf(parameters);
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(location, "location");
    }
}
