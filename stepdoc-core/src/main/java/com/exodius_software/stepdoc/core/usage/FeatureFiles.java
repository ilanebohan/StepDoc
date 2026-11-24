package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.util.FileTree;
import com.exodius_software.stepdoc.core.util.Whitespace;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.ParseError;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.Step;

/**
 * Finds {@code .feature} files and reads from them the steps Cucumber would run.
 *
 * <p>Steps are taken from the pickles of the Gherkin library, not from the raw lines: a pickle
 * is a scenario as Cucumber executes it, with background steps included and the placeholders
 * of a scenario outline replaced by the values of each example row.
 */
final class FeatureFiles {

    /**
     * A step as Cucumber matches it against step definitions.
     *
     * @param language Gherkin language of the feature file, which decides how numbers are written
     * @param text     step text without its keyword
     */
    record FeatureStep(String language, String text) {
    }

    /**
     * @param steps     distinct steps with the place each one first appears, in the order found
     * @param fileCount number of feature files found, parsable or not
     * @param problems  one per parse error
     */
    record Scan(Map<FeatureStep, SourceLocation> steps, int fileCount, List<Problem> problems) {
    }

    private static final String FEATURE_EXTENSION = ".feature";

    private FeatureFiles() {
    }

    /** Scans the directories recursively. A directory that does not exist is skipped. */
    static Scan scan(List<Path> directories) throws IOException {
        GherkinParser parser = GherkinParser.builder()
                .includeSource(false)
                .includeGherkinDocument(true)
                .includePickles(true)
                .build();
        Map<FeatureStep, SourceLocation> steps = new LinkedHashMap<>();
        List<Problem> problems = new ArrayList<>();
        int fileCount = 0;
        for (Path directory : directories) {
            for (FileTree.Entry file : featureFilesIn(directory)) {
                fileCount++;
                Map<String, Integer> lineByStepId = new HashMap<>();
                parser.parse(file.relativePath(), Files.readAllBytes(file.path())).forEach(envelope -> {
                    envelope.getGherkinDocument().flatMap(GherkinDocument::getFeature)
                            .ifPresent(feature -> writtenSteps(feature).forEach(step ->
                                    lineByStepId.put(step.getId(), Math.toIntExact(step.getLocation().getLine()))));
                    envelope.getPickle().ifPresent(pickle -> pickle.getSteps().forEach(step -> steps.putIfAbsent(
                            new FeatureStep(pickle.getLanguage(), step.getText()),
                            new SourceLocation(file.relativePath(), lineOf(step, lineByStepId)))));
                    envelope.getParseError().ifPresent(error -> problems.add(problem(file.relativePath(), error)));
                });
            }
        }
        return new Scan(steps, fileCount, problems);
    }

    private static List<FileTree.Entry> featureFilesIn(Path directory) throws IOException {
        return Files.isDirectory(directory) ? FileTree.filesWithExtension(directory, FEATURE_EXTENSION) : List.of();
    }

    /** Every step written in the file: in backgrounds and scenarios, at feature level or inside a rule. */
    private static List<Step> writtenSteps(Feature feature) {
        List<Step> steps = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            child.getBackground().ifPresent(background -> steps.addAll(background.getSteps()));
            child.getScenario().ifPresent(scenario -> steps.addAll(scenario.getSteps()));
            child.getRule().ifPresent(rule -> rule.getChildren().forEach(ruleChild -> {
                ruleChild.getBackground().ifPresent(background -> steps.addAll(background.getSteps()));
                ruleChild.getScenario().ifPresent(scenario -> steps.addAll(scenario.getSteps()));
            }));
        }
        return steps;
    }

    /** A pickle step points to the written step it comes from, then to its example row if any. */
    private static int lineOf(PickleStep step, Map<String, Integer> lineByStepId) {
        return step.getAstNodeIds().stream().findFirst().map(lineByStepId::get).orElse(1);
    }

    private static Problem problem(String relativePath, ParseError error) {
        int line = error.getSource().getLocation().map(Location::getLine).map(Math::toIntExact).orElse(1);
        String reason = Whitespace.collapse(error.getMessage());
        return new Problem(new SourceLocation(relativePath, line), "Feature file could not be parsed: " + reason);
    }
}