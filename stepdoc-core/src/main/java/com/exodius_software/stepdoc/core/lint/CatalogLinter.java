package com.exodius_software.stepdoc.core.lint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepParameter;
import com.exodius_software.stepdoc.core.usage.StepUsage;

/** Checks a catalog, and how feature files use it, against a set of rules. Stateless. */
public final class CatalogLinter {

    private static final Comparator<Violation> BY_LOCATION = Comparator
            .comparing((Violation violation) -> violation.location().file())
            .thenComparingInt(violation -> violation.location().line());

    private CatalogLinter() {
    }

    /**
     * Violations of the enabled rules, sorted by file then line so they read like a compiler report.
     *
     * @param usage what feature files say about the catalog; {@link StepUsage#NOT_REQUESTED} when
     *              {@link LintRule#UNUSED_STEP} is not enabled
     */
    public static List<Violation> lint(StepCatalog catalog, StepUsage usage, Set<LintRule> enabledRules) {
        List<Violation> violations = new ArrayList<>();
        for (LintRule rule : LintRule.values()) {
            if (enabledRules.contains(rule)) {
                violations.addAll(check(rule, catalog, usage));
            }
        }
        violations.sort(BY_LOCATION);
        return violations;
    }

    private static List<Violation> check(LintRule rule, StepCatalog catalog, StepUsage usage) {
        return switch (rule) {
            case UNDOCUMENTED_STEP -> undocumentedSteps(catalog);
            case UNDOCUMENTED_PARAMETER -> undocumentedParameters(catalog);
            case DUPLICATE_STEP -> duplicateSteps(catalog);
            case UNUSED_STEP -> unusedSteps(usage);
            case UNDEFINED_STEP -> undefinedSteps(usage);
            case AMBIGUOUS_STEP -> ambiguousSteps(usage);
            case SOURCE_PROBLEM -> sourceProblems(catalog, usage);
        };
    }

    private static List<Violation> undocumentedSteps(StepCatalog catalog) {
        return catalog.steps().stream()
                .filter(step -> step.description().isBlank())
                .map(step -> new Violation(LintRule.UNDOCUMENTED_STEP, step.location(),
                        "Step " + quoted(step) + " has no description"))
                .toList();
    }

    /**
     * Arguments without a name are skipped, as nothing can document them: either the method
     * does not declare them, which {@link LintRule#SOURCE_PROBLEM} reports, or the step is given
     * a method reference where a lambda is expected.
     */
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

    private static List<Violation> unusedSteps(StepUsage usage) {
        return usage.unusedSteps().stream()
                .map(step -> new Violation(LintRule.UNUSED_STEP, step.location(),
                        "Step " + quoted(step) + " is not used by any feature file"))
                .toList();
    }

    private static List<Violation> undefinedSteps(StepUsage usage) {
        return usage.undefinedSteps().stream()
                .map(step -> new Violation(LintRule.UNDEFINED_STEP, step.location(),
                        "Step \"" + step.text() + "\" has no step definition"))
                .toList();
    }

    private static List<Violation> ambiguousSteps(StepUsage usage) {
        return usage.ambiguousSteps().stream()
                .map(step -> new Violation(LintRule.AMBIGUOUS_STEP, step.location(), "Step \"" + step.text()
                        + "\" matches " + step.definitions().size() + " step definitions: "
                        + step.definitions().stream().map(CatalogLinter::located).collect(Collectors.joining(", "))))
                .toList();
    }

    private static String located(StepDefinition step) {
        return quoted(step) + " at " + step.location();
    }

    private static List<Violation> sourceProblems(StepCatalog catalog, StepUsage usage) {
        return Stream.concat(catalog.problems().stream(), usage.problems().stream())
                .map(problem -> new Violation(LintRule.SOURCE_PROBLEM, problem.location(), problem.message()))
                .toList();
    }

    private static String quoted(StepDefinition step) {
        return "\"" + step.expression() + "\"";
    }
}
