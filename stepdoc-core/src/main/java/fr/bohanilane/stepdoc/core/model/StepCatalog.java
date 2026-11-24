package fr.bohanilane.stepdoc.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a scan: every step found and every problem met on the way.
 * Order is deterministic (source path, then position in the file) so that
 * generated documentation only changes when the sources do.
 */
public record StepCatalog(List<StepDefinition> steps, List<Problem> problems) {

    public StepCatalog {
        steps = List.copyOf(steps);
        problems = List.copyOf(problems);
    }

    /** Concatenates catalogs, keeping the given order. */
    public static StepCatalog concat(List<StepCatalog> catalogs) {
        List<StepDefinition> steps = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        for (StepCatalog catalog : catalogs) {
            steps.addAll(catalog.steps());
            problems.addAll(catalog.problems());
        }
        return new StepCatalog(steps, problems);
    }
}
