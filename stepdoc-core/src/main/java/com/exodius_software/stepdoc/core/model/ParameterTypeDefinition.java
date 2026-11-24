package com.exodius_software.stepdoc.core.model;

import java.util.Objects;

/**
 * A parameter type declared by the scanned project: what {@code {color}} stands for in a step
 * text, when the project declares a {@code color} type with Cucumber's {@code @ParameterType}.
 *
 * @param name        name written between braces in step texts
 * @param regex       what the parameter matches, as Cucumber reads it
 * @param description what the type represents, empty when undocumented
 * @param location    position of the declaring annotation
 */
public record ParameterTypeDefinition(String name, String regex, String description, SourceLocation location) {

    public ParameterTypeDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(regex, "regex");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(location, "location");
    }
}