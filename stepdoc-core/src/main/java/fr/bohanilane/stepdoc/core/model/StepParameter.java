package fr.bohanilane.stepdoc.core.model;

import java.util.List;
import java.util.Objects;

/**
 * One argument of a step definition method.
 *
 * <p>Text fields are never {@code null}: an empty string means "not available"
 * (undocumented parameter, anonymous {@code {}} parameter, regex group, and so on).
 *
 * @param name           Java parameter name
 * @param javaType       Java type as written in the source, generics included
 * @param source         where Cucumber takes the value from
 * @param expressionType Cucumber Expression type without braces ({@code string}, {@code int},
 *                       a custom type); empty for regex groups and doc arguments
 * @param description    what the parameter represents
 * @param examples       example values, in declaration order
 */
public record StepParameter(
        String name,
        String javaType,
        Source source,
        String expressionType,
        String description,
        List<String> examples) {

    /** Where Cucumber takes the argument value from. */
    public enum Source {
        /** A parameter of the step text: Cucumber Expression placeholder or regex group. */
        EXPRESSION,
        /** The data table attached to the step. */
        DATA_TABLE,
        /** The doc string attached to the step. */
        DOC_STRING
    }

    public StepParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(javaType, "javaType");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(expressionType, "expressionType");
        Objects.requireNonNull(description, "description");
        examples = List.copyOf(examples);
    }
}
