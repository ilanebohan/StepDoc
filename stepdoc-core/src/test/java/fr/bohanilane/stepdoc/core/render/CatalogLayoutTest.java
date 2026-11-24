package fr.bohanilane.stepdoc.core.render;

import static fr.bohanilane.stepdoc.core.render.TestSteps.catalog;
import static fr.bohanilane.stepdoc.core.render.TestSteps.dataTable;
import static fr.bohanilane.stepdoc.core.render.TestSteps.expressionParameter;
import static fr.bohanilane.stepdoc.core.render.TestSteps.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.bohanilane.stepdoc.core.model.ExpressionSyntax;
import fr.bohanilane.stepdoc.core.model.SourceLocation;
import fr.bohanilane.stepdoc.core.model.StepCatalog;
import fr.bohanilane.stepdoc.core.model.StepDefinition;
import fr.bohanilane.stepdoc.core.model.StepKind;
import fr.bohanilane.stepdoc.core.model.StepParameter;
import fr.bohanilane.stepdoc.core.render.CatalogLayout.Section;

class CatalogLayoutTest {

    private final StepCatalog catalog = catalog(
            step(StepKind.THEN, "the cart is empty", "shop.CartSteps", 10),
            step(StepKind.WHEN, "I add a book", "shop.CartSteps", 20),
            step(StepKind.WHEN, "I remove a book", "shop.CartSteps", 30),
            step(StepKind.GIVEN, "an order", "shop.order.OrderSteps", 5),
            step(StepKind.GIVEN, "an empty cart", "shop.CartSteps", 40));

    @Test
    void groupsByClassInOrderOfAppearanceAndSortsByKeywordKeepingSourceOrder() {
        List<Section> sections = CatalogLayout.sections(catalog, GroupBy.CLASS);

        assertThat(sections).extracting(Section::title, Section::subtitle)
                .containsExactly(tuple("CartSteps", "shop.CartSteps"), tuple("OrderSteps", "shop.order.OrderSteps"));
        assertThat(sections.get(0).steps()).extracting(StepDefinition::expression)
                .containsExactly("an empty cart", "I add a book", "I remove a book", "the cart is empty");
    }

    @Test
    void omitsTheSubtitleOfAClassInTheDefaultPackage() {
        List<Section> sections = CatalogLayout.sections(catalog(step(StepKind.WHEN, "x", "CartSteps", 1)), GroupBy.CLASS);

        assertThat(sections).extracting(Section::subtitle).containsExactly("");
    }

    @Test
    void groupsByKeywordInGherkinOrderKeepingSourceOrder() {
        List<Section> sections = CatalogLayout.sections(catalog, GroupBy.KEYWORD);

        assertThat(sections).extracting(Section::title).containsExactly("Given", "When", "Then");
        assertThat(sections.get(0).steps()).extracting(StepDefinition::expression)
                .containsExactly("an order", "an empty cart");
    }

    @Test
    void labelsExpressionParametersWithThePlaceholderTheTestWriterTypes() {
        StepDefinition step = step(StepKind.WHEN, "I add {int} books for {}", "shop.CartSteps", 1);

        assertThat(CatalogLayout.typeLabel(step, expressionParameter("quantity", "int", "int", "")))
                .isEqualTo("{int}");
        assertThat(CatalogLayout.typeLabel(step, expressionParameter("anyone", "Object", "", "")))
                .isEqualTo("{}");
        assertThat(CatalogLayout.typeLabel(step, dataTable("books"))).isEqualTo("data table");
    }

    @Test
    void labelsRegexGroupsWithTheirJavaType() {
        StepParameter amount = new StepParameter("amount", "int", StepParameter.Source.EXPRESSION, "", "", List.of());
        StepDefinition regexStep = new StepDefinition("When", StepKind.WHEN, "^I pay (\\d+)$",
                ExpressionSyntax.REGULAR_EXPRESSION, "", List.of(amount), "shop.PaymentSteps", "pay",
                new SourceLocation("steps/PaymentSteps.java", 1));

        assertThat(CatalogLayout.typeLabel(regexStep, amount)).isEqualTo("int");
    }
}
