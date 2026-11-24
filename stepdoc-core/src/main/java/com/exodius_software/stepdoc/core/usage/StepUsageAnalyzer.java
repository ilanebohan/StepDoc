package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.usage.FeatureFiles.FeatureStep;
import com.exodius_software.stepdoc.core.usage.StepUsage.UndefinedStep;
import com.exodius_software.stepdoc.core.util.Whitespace;

import io.cucumber.cucumberexpressions.CucumberExpressionException;

/** Finds the step definitions no feature file uses, and the feature steps no definition implements. */
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
        Set<FeatureStep> definedSteps = new HashSet<>();
        for (StepDefinition definition : catalog.steps()) {
            try {
                List<FeatureStep> implemented = features.steps().keySet().stream()
                        .filter(step -> matcher.matches(definition, step))
                        .toList();
                if (implemented.isEmpty()) {
                    unusedSteps.add(definition);
                }
                definedSteps.addAll(implemented);
            } catch (CucumberExpressionException e) {
                // Usage unknown: report why, and do not call the step unused. Cucumber explains the
                // error over several lines, with a pointer under the faulty column: keep it on one.
                problems.add(new Problem(definition.location(), "Step \"" + definition.expression()
                        + "\" cannot be matched against feature files: " + Whitespace.collapse(e.getMessage())));
            }
        }
        List<UndefinedStep> undefinedSteps = features.steps().entrySet().stream()
                .filter(step -> !definedSteps.contains(step.getKey()))
                .map(step -> new UndefinedStep(step.getKey().text(), step.getValue()))
                .toList();
        return new StepUsage(unusedSteps, undefinedSteps, problems, "");
    }
}