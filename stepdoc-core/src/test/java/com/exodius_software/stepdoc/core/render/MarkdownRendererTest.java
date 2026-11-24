package com.exodius_software.stepdoc.core.render;

import static com.exodius_software.stepdoc.core.render.TestSteps.catalog;
import static com.exodius_software.stepdoc.core.render.TestSteps.catalogWithProblem;
import static com.exodius_software.stepdoc.core.render.TestSteps.expressionParameter;
import static com.exodius_software.stepdoc.core.render.TestSteps.step;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exodius_software.stepdoc.core.model.ExpressionSyntax;
import com.exodius_software.stepdoc.core.model.ParameterTypeDefinition;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepKind;
import com.exodius_software.stepdoc.core.model.StepParameter;

class MarkdownRendererTest {

    private final CatalogRenderer renderer = OutputFormat.MARKDOWN.renderer();

    @Test
    void rendersEachSectionAsATableWithEscapedCells() {
        String markdown = renderer.render(catalog(
                step(StepKind.WHEN, "I add {int} copies of {string}",
                        "Adds copies | bundles.\n\nUses <b>cart</b>.", "shop.CartSteps", 12,
                        expressionParameter("quantity", "int", "int", "number of copies", "1", "12"),
                        expressionParameter("title", "String", "string", ""))),
                RenderOptions.defaults());

        assertThat(markdown).isEqualTo("""
                # Step catalog

                1 step in 1 section.

                ## CartSteps

                `shop.CartSteps`

                | Keyword | Step | Description | Parameters | Source |
                | --- | --- | --- | --- | --- |
                | When | `I add {int} copies of {string}` | Adds copies \\| bundles.<br><br>Uses &lt;b&gt;cart&lt;/b&gt;. \
                | **quantity** `{int}` number of copies (e.g. `1`, `12`)<br>**title** `{string}` \
                | `steps/CartSteps.java:12` |
                """);
    }

    @Test
    void escapesPipesAndBackticksInsideCodeSpans() {
        String markdown = renderer.render(
                catalog(step(StepKind.THEN, "the label reads `a|b`", "shop.CartSteps", 1)),
                RenderOptions.defaults());

        assertThat(markdown).contains("| `` the label reads `a\\|b` `` |");
    }

    @Test
    void showsNoEmptyTypeForARegexGroupTheMethodDoesNotDeclare() {
        StepParameter missingArgument = new StepParameter("", "", StepParameter.Source.EXPRESSION, "", "", List.of());
        StepDefinition regexStep = new StepDefinition("When", StepKind.WHEN, "^I pay (\\d+)$",
                ExpressionSyntax.REGULAR_EXPRESSION, "Pays", List.of(missingArgument), "shop.PaymentSteps", "pay",
                new SourceLocation("steps/PaymentSteps.java", 1));

        assertThat(renderer.render(catalog(regexStep), RenderOptions.defaults()))
                .contains("| When | `^I pay (\\d+)$` | Pays |  | `steps/PaymentSteps.java:1` |");
    }

    @Test
    void leavesOutTheSourceColumnWhenDisabled() {
        RenderOptions options = new RenderOptions("Steps", GroupBy.CLASS, false, true, "");

        String markdown = renderer.render(catalog(step(StepKind.WHEN, "I add a book", "shop.CartSteps", 3)), options);

        assertThat(markdown)
                .contains("| Keyword | Step | Description | Parameters |\n")
                .contains("| When | `I add a book` |  |  |\n")
                .doesNotContain("CartSteps.java");
    }

    @Test
    void listsProblemsOnlyWhenEnabled() {
        var catalog = catalogWithProblem("steps/Broken.java", 4, "File could not be parsed");
        RenderOptions hidden = new RenderOptions("Steps", GroupBy.CLASS, true, false, "");

        assertThat(renderer.render(catalog, RenderOptions.defaults()))
                .contains("## Problems\n\n- `steps/Broken.java:4` File could not be parsed\n");
        assertThat(renderer.render(catalog, hidden)).doesNotContain("Problems");
    }

    @Test
    void showsWhatADeclaredParameterTypeMatches() {
        StepDefinition step = step(StepKind.WHEN, "I pick a {color} cover", "Picks a cover", "shop.CartSteps", 4,
                expressionParameter("color", "String", "color", "cover color"));
        ParameterTypeDefinition color = new ParameterTypeDefinition("color", "red|blue", "Color of a cover.",
                new SourceLocation("steps/Types.java", 9));

        String markdown = renderer.render(new StepCatalog(List.of(step), List.of(color), List.of()),
                RenderOptions.defaults());

        assertThat(markdown)
                .contains("| **color** `{color}` matching `red\\|blue` cover color |")
                .endsWith("""

                        ## Parameter types

                        | Name | Matches | Description | Source |
                        | --- | --- | --- | --- |
                        | `{color}` | `red\\|blue` | Color of a cover. | `steps/Types.java:9` |
                        """);
    }

    @Test
    void saysSoWhenThereIsNothingToDocument() {
        assertThat(renderer.render(catalog(), RenderOptions.defaults()))
                .isEqualTo("# Step catalog\n\nNo step definitions found.\n");
    }
}
