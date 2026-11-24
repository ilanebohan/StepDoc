package com.exodius_software.stepdoc.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.exodius_software.stepdoc.core.DocumentationChecker;
import com.exodius_software.stepdoc.core.lint.LintRule;
import com.exodius_software.stepdoc.core.lint.Violation;

/** Fails the build when step definitions break the configured documentation rules. */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractStepDocMojo {

    /**
     * Rules to apply, by id: {@code undocumented-step}, {@code undocumented-parameter},
     * {@code duplicate-step}, {@code unused-step}, {@code source-problem}. Empty means all of
     * them, so new rules apply without touching the configuration.
     */
    @Parameter(property = "stepdoc.rules")
    private List<String> rules;

    /**
     * Directories searched, recursively, for the {@code .feature} files that tell which steps
     * are used. Only read by the {@code unused-step} rule, which is skipped with a warning
     * when they contain no feature file.
     */
    @Parameter(property = "stepdoc.featureDirectories", defaultValue = "${project.basedir}/src/test/resources")
    private List<File> featureDirectories;

    /** When {@code false}, violations are only logged as warnings. */
    @Parameter(property = "stepdoc.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    @Override
    protected void run(Path sourceRoot) throws IOException, MojoFailureException {
        Set<LintRule> enabledRules = enabledRules();
        List<Path> featureRoots = featureDirectories.stream().map(File::toPath).toList();
        DocumentationChecker.Result result = new DocumentationChecker().check(sourceRoot, featureRoots, enabledRules);
        result.warnings().forEach(warning -> getLog().warn("StepDoc: " + warning));

        List<Violation> violations = result.violations();
        String checked = result.catalog().steps().size() + " step(s) checked against "
                + enabledRules.size() + " rule(s)";
        if (violations.isEmpty()) {
            getLog().info("StepDoc: " + checked + ", no violation");
            return;
        }
        String summary = "StepDoc: " + violations.size() + " violation(s), " + checked;
        if (failOnViolation) {
            violations.forEach(violation -> getLog().error(violation.toString()));
            throw new MojoFailureException(summary);
        }
        violations.forEach(violation -> getLog().warn(violation.toString()));
        getLog().warn(summary);
    }

    private Set<LintRule> enabledRules() {
        Set<LintRule> configured = rules == null ? Set.of() : rules.stream()
                .filter(rule -> !rule.isBlank())
                .map(LintRule::fromId)
                .collect(Collectors.toSet());
        return configured.isEmpty() ? EnumSet.allOf(LintRule.class) : configured;
    }
}
