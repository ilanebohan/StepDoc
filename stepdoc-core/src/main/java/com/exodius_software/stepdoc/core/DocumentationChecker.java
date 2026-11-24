package com.exodius_software.stepdoc.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.exodius_software.stepdoc.core.lint.CatalogLinter;
import com.exodius_software.stepdoc.core.lint.LintRule;
import com.exodius_software.stepdoc.core.lint.Violation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.parser.StepDefinitionParser;
import com.exodius_software.stepdoc.core.usage.StepUsage;
import com.exodius_software.stepdoc.core.usage.StepUsageAnalyzer;

/** Entry point for build tools: scan sources and feature files, apply the documentation rules. */
public final class DocumentationChecker {

    /**
     * What was found, so the caller can report it.
     *
     * @param violations rules broken, sorted by file then line
     * @param warnings   what the caller should tell the user without failing, such as a rule
     *                   that could not be applied
     */
    public record Result(StepCatalog catalog, List<Violation> violations, List<String> warnings) {

        public Result {
            violations = List.copyOf(violations);
            warnings = List.copyOf(warnings);
        }
    }

    private final StepDefinitionParser parser = new StepDefinitionParser();

    /**
     * @param featureDirectories searched for {@code .feature} files, and only read when
     *                           {@link LintRule#UNUSED_STEP} is enabled
     */
    public Result check(SourceSettings sources, List<Path> featureDirectories, Set<LintRule> enabledRules)
            throws IOException {
        StepCatalog catalog = parser.parseDirectory(sources.root(), sources.encoding());
        StepUsage usage = enabledRules.contains(LintRule.UNUSED_STEP)
                ? StepUsageAnalyzer.analyze(catalog, featureDirectories)
                : StepUsage.NOT_REQUESTED;
        List<String> warnings = usage.notAnalyzedReason().isEmpty()
                ? List.of()
                : List.of("Rule " + LintRule.UNUSED_STEP.id() + " was skipped: " + usage.notAnalyzedReason());
        return new Result(catalog, CatalogLinter.lint(catalog, usage, enabledRules), warnings);
    }
}
