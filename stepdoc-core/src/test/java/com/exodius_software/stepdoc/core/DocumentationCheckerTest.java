package com.exodius_software.stepdoc.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.exodius_software.stepdoc.core.lint.LintRule;
import com.exodius_software.stepdoc.core.lint.Violation;

class DocumentationCheckerTest {

    private static final Set<LintRule> ALL_RULES = EnumSet.allOf(LintRule.class);

    @TempDir
    Path workspace;

    private Path sources;
    private Path features;

    @BeforeEach
    void writeTwoDocumentedSteps() throws IOException {
        sources = Files.createDirectories(workspace.resolve("java"));
        features = Files.createDirectories(workspace.resolve("resources"));
        Files.writeString(sources.resolve("CartSteps.java"), """
                import io.cucumber.java.en.*;

                class CartSteps {
                    /** Starts with a cart containing nothing. */
                    @Given("an empty cart") void emptyCart() {}

                    /** Checks that the cart contains nothing. */
                    @Then("the cart is empty") void cartIsEmpty() {}
                }
                """);
    }

    @Test
    void reportsTheStepsThatTheFeatureFilesDoNotUse() throws IOException {
        Files.writeString(features.resolve("cart.feature"), """
                Feature: Cart
                  Scenario: Empty
                    Given an empty cart
                """);

        DocumentationChecker.Result result = check(ALL_RULES);

        assertThat(result.violations()).extracting(Violation::toString)
                .containsExactly("[unused-step] CartSteps.java:8 "
                        + "Step \"the cart is empty\" is not used by any feature file");
        assertThat(result.warnings()).isEmpty();
        assertThat(result.catalog().steps()).hasSize(2);
    }

    @Test
    void warnsThatUnusedStepsWereNotCheckedWhenThereIsNoFeatureFile() throws IOException {
        DocumentationChecker.Result result = check(ALL_RULES);

        assertThat(result.violations()).isEmpty();
        assertThat(result.warnings()).hasSize(3);
        assertThat(result.warnings().get(0)).startsWith("Rule unused-step was skipped: no .feature file found in ");
        assertThat(result.warnings().get(1)).startsWith("Rule undefined-step was skipped: no .feature file found in ");
        assertThat(result.warnings().get(2)).startsWith("Rule ambiguous-step was skipped: no .feature file found in ");
    }

    @Test
    void doesNotReadFeatureFilesWhenNoEnabledRuleNeedsThem() throws IOException {
        Files.writeString(features.resolve("broken.feature"), "This is not Gherkin");
        Set<LintRule> withoutFeatureRules =
                EnumSet.complementOf(EnumSet.of(LintRule.UNUSED_STEP, LintRule.UNDEFINED_STEP, LintRule.AMBIGUOUS_STEP));

        DocumentationChecker.Result result = check(withoutFeatureRules);

        assertThat(result.violations()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    private DocumentationChecker.Result check(Set<LintRule> rules) throws IOException {
        SourceSettings utf8Sources = new SourceSettings(List.of(sources), StandardCharsets.UTF_8);
        return new DocumentationChecker().check(utf8Sources, List.of(features), rules);
    }
}
