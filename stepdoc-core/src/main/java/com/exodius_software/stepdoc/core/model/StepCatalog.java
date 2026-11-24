package com.exodius_software.stepdoc.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a scan: every step and parameter type found, and every problem met on the way.
 * Order is deterministic (source path, then position in the file) so that
 * generated documentation only changes when the sources do.
 */
public record StepCatalog(
        List<StepDefinition> steps,
        List<ParameterTypeDefinition> parameterTypes,
        List<Problem> problems) {

    public StepCatalog {
        steps = List.copyOf(steps);
        parameterTypes = List.copyOf(parameterTypes);
        problems = List.copyOf(problems);
    }

    /** A catalog of a project that declares no parameter type. */
    public StepCatalog(List<StepDefinition> steps, List<Problem> problems) {
        this(steps, List.of(), problems);
    }

    /** Concatenates catalogs, keeping the given order. */
    public static StepCatalog concat(List<StepCatalog> catalogs) {
        List<StepDefinition> steps = new ArrayList<>();
        List<ParameterTypeDefinition> parameterTypes = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        for (StepCatalog catalog : catalogs) {
            steps.addAll(catalog.steps());
            parameterTypes.addAll(catalog.parameterTypes());
            problems.addAll(catalog.problems());
        }
        return new StepCatalog(steps, parameterTypes, problems);
    }
}