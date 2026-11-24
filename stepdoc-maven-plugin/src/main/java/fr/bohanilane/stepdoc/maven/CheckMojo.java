package fr.bohanilane.stepdoc.maven;

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

import fr.bohanilane.stepdoc.core.lint.CatalogLinter;
import fr.bohanilane.stepdoc.core.lint.LintRule;
import fr.bohanilane.stepdoc.core.lint.Violation;
import fr.bohanilane.stepdoc.core.model.StepCatalog;
import fr.bohanilane.stepdoc.core.parser.StepDefinitionParser;

/** Fails the build when step definitions break the configured documentation rules. */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractStepDocMojo {

    /**
     * Rules to apply, by id: {@code undocumented-step}, {@code undocumented-parameter},
     * {@code duplicate-step}, {@code source-problem}. Empty means all of them,
     * so new rules apply without touching the configuration.
     */
    @Parameter(property = "stepdoc.rules")
    private List<String> rules;

    /** When {@code false}, violations are only logged as warnings. */
    @Parameter(property = "stepdoc.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    @Override
    protected void run(Path sourceRoot) throws IOException, MojoFailureException {
        Set<LintRule> enabledRules = enabledRules();
        StepCatalog catalog = new StepDefinitionParser().parseDirectory(sourceRoot);
        List<Violation> violations = CatalogLinter.lint(catalog, enabledRules);

        String checked = catalog.steps().size() + " step(s) checked against " + enabledRules.size() + " rule(s)";
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
