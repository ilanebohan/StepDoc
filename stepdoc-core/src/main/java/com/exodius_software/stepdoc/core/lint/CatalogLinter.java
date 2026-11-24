package com.exodius_software.stepdoc.core.lint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepParameter;

/** Checks a catalog against a set of rules. Stateless. */
public final class CatalogLinter {

    private static final Comparator<Violation> BY_LOCATION = Comparator
            .comparing((Violation violation) -> violation.location().file())
            .thenComparingInt(violation -> violation.location().line());

    private CatalogLinter() {
    }

    /** Violations of the enabled rules, sorted by file then line so they read like a compiler report. */
    public static List<Violation> lint(StepCatalog catalog, Set<LintRule> enabledRules) {
        List<Violation> violations = new ArrayList<>();
        for (LintRule rule : LintRule.values()) {
            if (enabledRules.contains(rule)) {
                violations.addAll(check(rule, catalog));
            }
        }
        violations.sort(BY_LOCATION);
        return violations;
    }

    private static List<Violation> check(LintRule rule, StepCatalog catalog) {
        return switch (rule) {
            case UNDOCUMENTED_STEP -> undocumentedSteps(catalog);
            case UNDOCUMENTED_PARAMETER -> undocumentedParameters(catalog);
            case DUPLICATE_STEP -> duplicateSteps(catalog);
            case SOURCE_PROBLEM -> sourceProblems(catalog);
        };
    }

    private static List<Violation> undocumentedSteps(StepCatalog catalog) {
        return catalog.steps().stream()
                .filter(step -> step.description().isBlank())
                .map(step -> new Violation(LintRule.UNDOCUMENTED_STEP, step.location(),
                        "Step " + quoted(step) + " has no description"))
                .toList();
    }

    /** Arguments the method does not declare are skipped: {@link LintRule#SOURCE_PROBLEM} already covers them. */
    private static List<Violation> undocumentedParameters(StepCatalog catalog) {
        List<Violation> violations = new ArrayList<>();
        for (StepDefinition step : catalog.steps()) {
            for (StepParameter parameter : step.parameters()) {
                if (!parameter.name().isEmpty() && parameter.description().isBlank()) {
                    violations.add(new Violation(LintRule.UNDOCUMENTED_PARAMETER, step.location(),
                            "Parameter '" + parameter.name() + "' of step " + quoted(step) + " has no description"));
                }
            }
        }
        return violations;
    }

    /** The keyword does not matter: Cucumber matches step text only, so the same expression conflicts. */
    private static List<Violation> duplicateSteps(StepCatalog catalog) {
        Map<String, StepDefinition> firstByExpression = new HashMap<>();
        List<Violation> violations = new ArrayList<>();
        for (StepDefinition step : catalog.steps()) {
            StepDefinition first = firstByExpression.putIfAbsent(step.expression(), step);
            if (first != null) {
                violations.add(new Violation(LintRule.DUPLICATE_STEP, step.location(),
                        "Step " + quoted(step) + " is already defined at " + first.location()));
            }
        }
        return violations;
    }

    private static List<Violation> sourceProblems(StepCatalog catalog) {
        return catalog.problems().stream()
                .map(problem -> new Violation(LintRule.SOURCE_PROBLEM, problem.location(), problem.message()))
                .toList();
    }

    private static String quoted(StepDefinition step) {
        return "\"" + step.expression() + "\"";
    }
}
