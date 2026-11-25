package com.exodius_software.stepdoc.core.usage;

import java.util.List;
import java.util.Objects;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepDefinition;

/**
 * What the feature files and the step definitions of a catalog say about each other.
 *
 * <p>Calling a step unused, undefined or ambiguous is an accusation: it is only made when
 * every feature file could be read. Otherwise the three lists are empty and
 * {@code notAnalyzedReason} says why.
 *
 * @param unusedSteps       step definitions that no feature file uses, in catalog order
 * @param undefinedSteps    steps of feature files that no step definition implements, each
 *                          reported once, in the order they were found
 * @param ambiguousSteps    steps of feature files that several step definitions implement,
 *                          each reported once, in the order they were found
 * @param problems          feature files that cannot be parsed, expressions and parameter
 *                          types that cannot be matched
 * @param notAnalyzedReason why usage could not be determined, empty when it was
 */
public record StepUsage(
        List<StepDefinition> unusedSteps,
        List<UndefinedStep> undefinedSteps,
        List<AmbiguousStep> ambiguousSteps,
        List<Problem> problems,
        String notAnalyzedReason) {

    /**
     * A step of a feature file that no step definition implements.
     *
     * @param text     step text without its keyword, outline placeholders replaced by their values
     * @param location where the step first appears, relative to its feature directory
     */
    public record UndefinedStep(String text, SourceLocation location) {

        public UndefinedStep {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(location, "location");
        }
    }

    /**
     * A step of a feature file that Cucumber cannot run, because it would not know which of
     * several step definitions to call.
     *
     * @param text        step text without its keyword, outline placeholders replaced by their values
     * @param location    where the step first appears, relative to its feature directory
     * @param definitions the definitions that match it, one per step text, in catalog order
     */
    public record AmbiguousStep(String text, SourceLocation location, List<StepDefinition> definitions) {

        public AmbiguousStep {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(location, "location");
            definitions = List.copyOf(definitions);
        }
    }

    /** For callers that do not need usage: nothing to accuse, nothing to report. */
    public static final StepUsage NOT_REQUESTED = new StepUsage(List.of(), List.of(), List.of(), List.of(), "");

    public StepUsage {
        unusedSteps = List.copyOf(unusedSteps);
        undefinedSteps = List.copyOf(undefinedSteps);
        ambiguousSteps = List.copyOf(ambiguousSteps);
        problems = List.copyOf(problems);
        Objects.requireNonNull(notAnalyzedReason, "notAnalyzedReason");
    }

    static StepUsage notAnalyzed(String reason, List<Problem> problems) {
        return new StepUsage(List.of(), List.of(), List.of(), problems, reason);
    }
}