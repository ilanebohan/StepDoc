package com.exodius_software.stepdoc.core.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.parser.StepDefinitionParser;

class StepUsageAnalyzerTest {

    private static final String CART_STEPS = """
            import io.cucumber.java.en.*;

            class CartSteps {
                @Given("an empty cart") void emptyCart() {}
                @When("I add {int} copies of {string}") void add(int quantity, String title) {}
                @Then("^the total is (\\\\d+) euros$") void total(int euros) {}
                @Then("the cart is empty") void cartIsEmpty() {}
            }
            """;

    @TempDir
    Path features;

    @Test
    void reportsOnlyTheStepsNoScenarioUses() throws IOException {
        write(features, "cart.feature", """
                Feature: Cart
                  Scenario: Adding books
                    Given an empty cart
                    When I add 2 copies of "Dune"
                    Then the total is 20 euros
                """);

        StepUsage usage = analyze(CART_STEPS, features);

        assertThat(unusedExpressions(usage)).containsExactly("the cart is empty");
        assertThat(usage.problems()).isEmpty();
        assertThat(usage.notAnalyzedReason()).isEmpty();
    }

    @Test
    void countsBackgroundStepsAndOutlineStepsWithTheirExampleValues() throws IOException {
        write(features, "cart.feature", """
                Feature: Cart
                  Background:
                    Given an empty cart

                  Scenario Outline: Adding books
                    When I add <quantity> copies of "<title>"
                    Then the total is <total> euros

                    Examples:
                      | quantity | title | total |
                      | 2        | Dune  | 20    |
                """);

        StepUsage usage = analyze(CART_STEPS, features);

        assertThat(unusedExpressions(usage)).containsExactly("the cart is empty");
    }

    @Test
    void readsFeatureFilesWrittenInAnotherGherkinLanguage() throws IOException {
        write(features, "cart.feature", """
                # language: de
                Funktion: Cart
                  Szenario: Empty cart
                    Angenommen an empty cart
                    Dann the cart is empty
                """);

        StepUsage usage = analyze(CART_STEPS, features);

        assertThat(unusedExpressions(usage))
                .containsExactly("I add {int} copies of {string}", "^the total is (\\d+) euros$");
    }

    @Test
    void readsNumbersTheWayTheLanguageOfTheFeatureWritesThem() throws IOException {
        write(features, "price.feature", """
                # language: de
                Funktion: Price
                  Szenario: German number
                    Dann the price is 1.000,50 euros
                """);

        StepUsage usage = analyze("""
                import io.cucumber.java.en.Then;

                class PriceSteps {
                    @Then("the price is {float} euros") void price(float euros) {}
                }
                """, features);

        assertThat(usage.unusedSteps()).isEmpty();
    }

    @Test
    void assumesACustomParameterTypeMatchesAnything() throws IOException {
        write(features, "cover.feature", """
                Feature: Cover
                  Scenario: Picking a cover
                    When I pick the red cover
                """);

        StepUsage usage = analyze("""
                import io.cucumber.java.en.*;

                class CoverSteps {
                    @When("I pick the {color} cover") void pick(String color) {}
                    @When("I choose a {size} box") void choose(String size) {}
                }
                """, features);

        assertThat(unusedExpressions(usage)).containsExactly("I choose a {size} box");
        assertThat(usage.problems()).isEmpty();
    }

    @Test
    void searchesEveryDirectoryRecursivelyAndSkipsMissingOnes(@TempDir Path workspace) throws IOException {
        Path first = workspace.resolve("first");
        Path second = workspace.resolve("second");
        write(first, "cart/empty.feature", """
                Feature: Empty cart
                  Scenario: Empty
                    Given an empty cart
                """);
        write(second, "total.feature", """
                Feature: Total
                  Scenario: Total
                    Then the total is 20 euros
                """);

        StepUsage usage = analyze(CART_STEPS, first, workspace.resolve("missing"), second);

        assertThat(unusedExpressions(usage))
                .containsExactly("I add {int} copies of {string}", "the cart is empty");
    }

    @Test
    void callsNoStepUnusedWithoutAnyFeatureFile() throws IOException {
        write(features, "notes.txt", "Given an empty cart");

        StepUsage usage = analyze(CART_STEPS, features);

        assertThat(usage.unusedSteps()).isEmpty();
        assertThat(usage.notAnalyzedReason()).startsWith("no .feature file found in ");
    }

    @Test
    void callsNoStepUnusedWhenAFeatureFileCannotBeParsed() throws IOException {
        write(features, "broken.feature", """
                Feature: Cart
                  Scenario: Broken
                    Given an empty cart
                  This line is not Gherkin
                """);
        write(features, "valid.feature", """
                Feature: Total
                  Scenario: Total
                    Then the total is 20 euros
                """);

        StepUsage usage = analyze(CART_STEPS, features);

        assertThat(usage.unusedSteps()).isEmpty();
        assertThat(usage.notAnalyzedReason()).isEqualTo("some feature files could not be parsed");
        assertThat(usage.problems()).singleElement().satisfies(problem -> {
            assertThat(problem.location()).isEqualTo(new SourceLocation("broken.feature", 4));
            assertThat(problem.message()).startsWith("Feature file could not be parsed: ").doesNotContain("\n");
        });
    }

    @Test
    void reportsAnExpressionItCannotMatchInsteadOfCallingItUnused() throws IOException {
        write(features, "cart.feature", """
                Feature: Cart
                  Scenario: Empty
                    Given an empty cart
                """);

        StepUsage usage = analyze("""
                import io.cucumber.java.en.*;

                class CartSteps {
                    @Given("an empty cart") void emptyCart() {}
                    @When("I add {int books") void add() {}
                }
                """, features);

        assertThat(usage.unusedSteps()).isEmpty();
        assertThat(usage.problems()).singleElement().satisfies(problem -> {
            assertThat(problem.location()).isEqualTo(new SourceLocation("CartSteps.java", 5));
            assertThat(problem.message())
                    .startsWith("Step \"I add {int books\" cannot be matched against feature files: ")
                    .doesNotContain("\n");
        });
    }

    @Test
    void matchesADeclaredParameterTypeWithItsOwnRegex() throws IOException {
        String coverSteps = """
                import io.cucumber.java.ParameterType;
                import io.cucumber.java.en.When;

                class CoverSteps {
                    @ParameterType("red|blue") public String color(String value) { return value; }
                    @When("I pick a {color} cover") public void pick(String color) {}
                }
                """;
        write(features, "cover.feature", """
                Feature: Covers
                  Scenario: Picking
                    When I pick a green cover
                """);

        assertThat(unusedExpressions(analyze(coverSteps, features))).containsExactly("I pick a {color} cover");

        write(features, "cover.feature", """
                Feature: Covers
                  Scenario: Picking
                    When I pick a blue cover
                """);

        assertThat(unusedExpressions(analyze(coverSteps, features))).isEmpty();
    }

    @Test
    void reportsADeclaredParameterTypeCucumberWouldRefuse() throws IOException {
        write(features, "cart.feature", """
                Feature: Cart
                  Scenario: Empty
                    Given an empty cart
                """);

        StepUsage usage = analyze("""
                import io.cucumber.java.ParameterType;
                import io.cucumber.java.en.Given;

                class CartSteps {
                    @ParameterType(value = "\\\\d+", name = "int") public String number(String value) { return value; }
                    @Given("an empty cart") public void emptyCart() {}
                }
                """, features);

        assertThat(usage.problems()).singleElement().satisfies(problem -> {
            assertThat(problem.location()).isEqualTo(new SourceLocation("CartSteps.java", 5));
            assertThat(problem.message()).startsWith("Parameter type 'int' cannot be used: ");
        });
        assertThat(usage.unusedSteps()).isEmpty();
    }

    @Test
    void readsAFeatureFileThatDeclaresItsOwnEncoding() throws IOException {
        Files.write(features.resolve("cafe.feature"), """
                # encoding: iso-8859-1
                Feature: Caf\u00e9
                  Scenario: Ordering
                    When I order a caf\u00e9
                """.getBytes(StandardCharsets.ISO_8859_1));

        StepUsage usage = analyze("""
                import io.cucumber.java.en.When;
                class CafeSteps { @When("I order a caf\u00e9") void order() {} }
                """, features);

        assertThat(usage.unusedSteps()).isEmpty();
        assertThat(usage.problems()).isEmpty();
    }

    private static StepUsage analyze(String stepSource, Path... featureDirectories) throws IOException {
        StepCatalog catalog = new StepDefinitionParser().parseSource("CartSteps.java", stepSource);
        return StepUsageAnalyzer.analyze(catalog, List.of(featureDirectories));
    }

    private static List<String> unusedExpressions(StepUsage usage) {
        return usage.unusedSteps().stream().map(StepDefinition::expression).toList();
    }

    private static void write(Path directory, String relativePath, String content) throws IOException {
        Path file = directory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
