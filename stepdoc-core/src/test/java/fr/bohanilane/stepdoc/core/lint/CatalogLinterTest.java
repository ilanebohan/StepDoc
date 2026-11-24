package fr.bohanilane.stepdoc.core.lint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.bohanilane.stepdoc.core.model.ExpressionSyntax;
import fr.bohanilane.stepdoc.core.model.Problem;
import fr.bohanilane.stepdoc.core.model.SourceLocation;
import fr.bohanilane.stepdoc.core.model.StepCatalog;
import fr.bohanilane.stepdoc.core.model.StepDefinition;
import fr.bohanilane.stepdoc.core.model.StepKind;
import fr.bohanilane.stepdoc.core.model.StepParameter;

class CatalogLinterTest {

    private static final String FILE = "steps/CartSteps.java";
    private static final Set<LintRule> ALL_RULES = EnumSet.allOf(LintRule.class);

    @Test
    void acceptsAFullyDocumentedCatalog() {
        StepCatalog catalog = catalog(step("I add {int} books", "Adds books", 3,
                parameter("quantity", "number of copies")));

        assertThat(CatalogLinter.lint(catalog, ALL_RULES)).isEmpty();
    }

    @Test
    void reportsAStepWithoutDescription() {
        StepCatalog catalog = catalog(step("I add a book", " ", 7));

        assertThat(CatalogLinter.lint(catalog, ALL_RULES)).extracting(Violation::toString)
                .containsExactly("[undocumented-step] steps/CartSteps.java:7 Step \"I add a book\" has no description");
    }

    @Test
    void reportsEachUndocumentedParameterButNotAnArgumentTheMethodLacks() {
        StepCatalog catalog = catalog(step("I add {int} copies of {string} for {}", "Adds books", 4,
                parameter("quantity", "number of copies"),
                parameter("title", ""),
                parameter("", "")));

        assertThat(CatalogLinter.lint(catalog, ALL_RULES)).extracting(Violation::message)
                .containsExactly("Parameter 'title' of step \"I add {int} copies of {string} for {}\" has no description");
    }

    @Test
    void reportsEveryRepeatOfAnExpressionWhateverTheKeyword() {
        StepCatalog catalog = catalog(
                step(StepKind.GIVEN, "a book in the cart", 2),
                step(StepKind.GIVEN, "an empty cart", 5),
                step(StepKind.THEN, "a book in the cart", 9),
                step(StepKind.WHEN, "a book in the cart", 12));

        assertThat(CatalogLinter.lint(catalog, Set.of(LintRule.DUPLICATE_STEP)))
                .extracting(violation -> violation.location().line(), Violation::message)
                .containsExactly(
                        tuple(9, "Step \"a book in the cart\" is already defined at steps/CartSteps.java:2"),
                        tuple(12, "Step \"a book in the cart\" is already defined at steps/CartSteps.java:2"));
    }

    @Test
    void turnsParserProblemsIntoViolations() {
        StepCatalog catalog = new StepCatalog(List.of(),
                List.of(new Problem(new SourceLocation("steps/Broken.java", 1), "File could not be parsed")));

        assertThat(CatalogLinter.lint(catalog, ALL_RULES)).extracting(Violation::toString)
                .containsExactly("[source-problem] steps/Broken.java:1 File could not be parsed");
    }

    @Test
    void appliesOnlyTheEnabledRules() {
        StepCatalog catalog = catalog(step("I add {int} books", "", 3, parameter("quantity", "")));

        assertThat(CatalogLinter.lint(catalog, Set.of(LintRule.UNDOCUMENTED_PARAMETER)))
                .extracting(Violation::rule)
                .containsExactly(LintRule.UNDOCUMENTED_PARAMETER);
        assertThat(CatalogLinter.lint(catalog, Set.of())).isEmpty();
    }

    @Test
    void sortsViolationsByFileThenLineAcrossRules() {
        StepCatalog catalog = new StepCatalog(
                List.of(step("I add a book", "", 30), step("I remove a book", "", 4)),
                List.of(new Problem(new SourceLocation("steps/A.java", 50), "File could not be parsed"),
                        new Problem(new SourceLocation(FILE, 12), "Parameter count mismatch")));

        assertThat(CatalogLinter.lint(catalog, ALL_RULES))
                .extracting(violation -> violation.location().toString())
                .containsExactly("steps/A.java:50", FILE + ":4", FILE + ":12", FILE + ":30");
    }

    @Test
    void readsRuleIdsFromConfiguration() {
        assertThat(LintRule.fromId(" Undocumented-Step ")).isEqualTo(LintRule.UNDOCUMENTED_STEP);
        assertThat(LintRule.DUPLICATE_STEP.id()).isEqualTo("duplicate-step");
        assertThatThrownBy(() -> LintRule.fromId("unused-step"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown rule 'unused-step', expected one of: "
                        + "undocumented-step, undocumented-parameter, duplicate-step, source-problem");
    }

    private static StepCatalog catalog(StepDefinition... steps) {
        return new StepCatalog(List.of(steps), List.of());
    }

    private static StepDefinition step(StepKind kind, String expression, int line) {
        return new StepDefinition(kind.name(), kind, expression, ExpressionSyntax.CUCUMBER_EXPRESSION,
                "Documented", List.of(), "shop.CartSteps", "step" + line, new SourceLocation(FILE, line));
    }

    private static StepDefinition step(String expression, String description, int line, StepParameter... parameters) {
        return new StepDefinition("When", StepKind.WHEN, expression, ExpressionSyntax.CUCUMBER_EXPRESSION,
                description, List.of(parameters), "shop.CartSteps", "step" + line, new SourceLocation(FILE, line));
    }

    private static StepParameter parameter(String name, String description) {
        return new StepParameter(name, "String", StepParameter.Source.EXPRESSION, "string", description, List.of());
    }
}
