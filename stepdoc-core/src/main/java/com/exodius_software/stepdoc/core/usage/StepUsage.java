package com.exodius_software.stepdoc.core.usage;

import java.util.List;
import java.util.Objects;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepDefinition;

/**
 * What the feature files say about the step definitions of a catalog.
 *
 * <p>Calling a step unused is an accusation: it is only made when every feature file could be
 * read. Otherwise {@code unusedSteps} is empty and {@code notAnalyzedReason} says why.
 *
 * @param unusedSteps       steps that no feature file uses, in catalog order
 * @param problems          feature files that cannot be parsed, step expressions that cannot
 *                          be matched
 * @param notAnalyzedReason why unused steps could not be determined, empty when they were
 */
public record StepUsage(List<StepDefinition> unusedSteps, List<Problem> problems, String notAnalyzedReason) {

    /** For callers that do not need usage: nothing unused, nothing to report. */
    public static final StepUsage NOT_REQUESTED = new StepUsage(List.of(), List.of(), "");

    public StepUsage {
        unusedSteps = List.copyOf(unusedSteps);
        problems = List.copyOf(problems);
        Objects.requireNonNull(notAnalyzedReason, "notAnalyzedReason");
    }

    static StepUsage notAnalyzed(String reason, List<Problem> problems) {
        return new StepUsage(List.of(), problems, reason);
    }
}
