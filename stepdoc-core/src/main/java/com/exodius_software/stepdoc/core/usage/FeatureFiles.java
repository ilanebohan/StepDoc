package com.exodius_software.stepdoc.core.usage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;

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
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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
            for (Path file : featureFilesIn(directory)) {
                fileCount++;
                String relativePath = directory.relativize(file).toString().replace('\\', '/');
                parser.parse(relativePath, Files.readAllBytes(file)).forEach(envelope -> {
                    envelope.getPickle().ifPresent(pickle -> pickle.getSteps().forEach(step ->
                            steps.add(new FeatureStep(pickle.getLanguage(), step.getText()))));
                    envelope.getParseError().ifPresent(error -> problems.add(problem(relativePath, error)));
                });
            }
        }
        return new Scan(steps, fileCount, problems);
    }

    private static List<Path> featureFilesIn(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(FEATURE_EXTENSION))
                    .sorted(Comparator.comparing(path -> directory.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }
    }

    private static Problem problem(String relativePath, ParseError error) {
        int line = error.getSource().getLocation().map(Location::getLine).orElse(1);
        String reason = WHITESPACE.matcher(error.getMessage()).replaceAll(" ").trim();
        return new Problem(new SourceLocation(relativePath, line), "Feature file could not be parsed: " + reason);
    }
}
