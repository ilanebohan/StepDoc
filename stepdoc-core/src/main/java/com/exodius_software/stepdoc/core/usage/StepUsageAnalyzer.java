package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;

import io.cucumber.cucumberexpressions.CucumberExpressionException;

/** Finds the step definitions that no feature file uses. */
public final class StepUsageAnalyzer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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
        StepMatcher matcher = new StepMatcher(features.steps());
        List<StepDefinition> unusedSteps = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        for (StepDefinition step : catalog.steps()) {
            try {
                if (!matcher.isUsed(step)) {
                    unusedSteps.add(step);
                }
            } catch (CucumberExpressionException e) {
                // Usage unknown: report why, and do not call the step unused.
                problems.add(new Problem(step.location(), "Step \"" + step.expression()
                        + "\" cannot be matched against feature files: " + singleLine(e.getMessage())));
            }
        }
        return new StepUsage(unusedSteps, problems, "");
    }

    /** Cucumber explains expression errors over several lines, with a pointer under the faulty column. */
    private static String singleLine(String message) {
        return WHITESPACE.matcher(message).replaceAll(" ").trim();
    }
}
