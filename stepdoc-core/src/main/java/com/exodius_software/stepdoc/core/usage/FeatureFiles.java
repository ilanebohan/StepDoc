package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.util.FileTree;
import com.exodius_software.stepdoc.core.util.Whitespace;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.ParseError;

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
     * @param steps     distinct steps, in the order they were found
     * @param fileCount number of feature files found, parsable or not
     * @param problems  one per parse error
     */
    record Scan(Set<FeatureStep> steps, int fileCount, List<Problem> problems) {
    }

    private static final String FEATURE_EXTENSION = ".feature";

    private FeatureFiles() {
    }

    /** Scans the directories recursively. A directory that does not exist is skipped. */
    static Scan scan(List<Path> directories) throws IOException {
        GherkinParser parser = GherkinParser.builder()
                .includeSource(false)
                .includeGherkinDocument(false)
                .includePickles(true)
                .build();
        Set<FeatureStep> steps = new LinkedHashSet<>();
        List<Problem> problems = new ArrayList<>();
        int fileCount = 0;
        for (Path directory : directories) {
            for (FileTree.Entry file : featureFilesIn(directory)) {
                fileCount++;
                parser.parse(file.relativePath(), Files.readAllBytes(file.path())).forEach(envelope -> {
                    envelope.getPickle().ifPresent(pickle -> pickle.getSteps().forEach(step ->
                            steps.add(new FeatureStep(pickle.getLanguage(), step.getText()))));
                    envelope.getParseError().ifPresent(error -> problems.add(problem(file.relativePath(), error)));
                });
            }
        }
        return new Scan(steps, fileCount, problems);
    }

    private static List<FileTree.Entry> featureFilesIn(Path directory) throws IOException {
        return Files.isDirectory(directory) ? FileTree.filesWithExtension(directory, FEATURE_EXTENSION) : List.of();
    }

    private static Problem problem(String relativePath, ParseError error) {
        int line = error.getSource().getLocation().map(Location::getLine).orElse(1);
        String reason = Whitespace.collapse(error.getMessage());
        return new Problem(new SourceLocation(relativePath, line), "Feature file could not be parsed: " + reason);
    }
}
