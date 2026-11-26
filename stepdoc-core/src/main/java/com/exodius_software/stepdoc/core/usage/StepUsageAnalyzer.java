package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.usage.FeatureFiles.FeatureStep;
import com.exodius_software.stepdoc.core.usage.StepUsage.AmbiguousStep;
import com.exodius_software.stepdoc.core.usage.StepUsage.UndefinedStep;
import com.exodius_software.stepdoc.core.util.Whitespace;

import io.cucumber.cucumberexpressions.CucumberExpressionException;

/**
 * Compares the steps of feature files with the step definitions of a catalog: which
 * definitions are never used, which steps have no definition, which have several.
 */
public final class StepUsageAnalyzer {

    private StepUsageAnalyzer() {
    }

    /**
     * Matches every step of the feature files against the step definitions of the catalog.
     *
     * @param featureDirectories searched recursively for {@code .feature} files; a directory
     *                           that does not exist is skipped
     */
    public static StepUsage analyze(StepCatalog catalog, List<Path> featureDirectories) throws IOException {
        FeatureFiles.Scan features = FeatureFiles.scan(featureDirectories);
        if (features.fileCount() == 0) {
            return StepUsage.notAnalyzed("no .feature file found in " + featureDirectories, features.problems());
        }
        if (!features.problems().isEmpty()) {
            return StepUsage.notAnalyzed("some feature files could not be parsed", features.problems());
        }
        StepMatcher matcher = new StepMatcher(catalog.parameterTypes());
        List<Problem> problems = new ArrayList<>(matcher.parameterTypeProblems());
        List<StepDefinition> unusedSteps = new ArrayList<>();
        Map<FeatureStep, List<StepDefinition>> definitionsByStep = new LinkedHashMap<>();
        features.steps().keySet().forEach(step -> definitionsByStep.put(step, new ArrayList<>()));
        for (StepDefinition definition : catalog.steps()) {
            try {
                List<FeatureStep> implemented = features.steps().keySet().stream()
                        .filter(step -> matcher.matches(definition, step))
                        .toList();
                if (implemented.isEmpty()) {
                    unusedSteps.add(definition);
                }
                implemented.forEach(step -> definitionsByStep.get(step).add(definition));
            } catch (CucumberExpressionException e) {
                // Usage unknown: report why, and do not call the step unused. Cucumber explains the
                // error over several lines, with a pointer under the faulty column: keep it on one.
                problems.add(new Problem(definition.location(), "Step \"" + definition.expression()
                        + "\" cannot be matched against feature files: " + Whitespace.collapse(e.getMessage())));
            }
        }
        List<UndefinedStep> undefinedSteps = new ArrayList<>();
        List<AmbiguousStep> ambiguousSteps = new ArrayList<>();
        definitionsByStep.forEach((step, definitions) -> {
            SourceLocation location = features.steps().get(step);
            List<StepDefinition> rivals = rivals(definitions, step, matcher);
            if (definitions.isEmpty()) {
                undefinedSteps.add(new UndefinedStep(step.text(), location));
            } else if (rivals.size() > 1) {
                ambiguousSteps.add(new AmbiguousStep(step.text(), location, rivals));
            }
        });
        return new StepUsage(unusedSteps, undefinedSteps, ambiguousSteps, problems, "");
    }

    /**
     * The definitions Cucumber would hesitate between for {@code step}, among those that match it.
     *
     * <p>Two are left out. A definition that only matches as long as an undeclared parameter
     * type accepts the text: its real regular expression may well refuse it. And a definition
     * with the same text as an earlier one: that is a duplicate, which has its own rule.
     */
    private static List<StepDefinition> rivals(List<StepDefinition> matching, FeatureStep step, StepMatcher matcher) {
        Map<String, StepDefinition> firstByExpression = new LinkedHashMap<>();
        for (StepDefinition definition : matching) {
            if (!matcher.assumesAParameterType(definition, step)) {
                firstByExpression.putIfAbsent(definition.expression(), definition);
            }
        }
        return List.copyOf(firstByExpression.values());
    }
}