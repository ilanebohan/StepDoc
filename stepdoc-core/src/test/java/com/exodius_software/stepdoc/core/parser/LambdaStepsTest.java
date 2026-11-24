package com.exodius_software.stepdoc.core.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import com.exodius_software.stepdoc.core.model.ParameterTypeDefinition;
import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepKind;
import com.exodius_software.stepdoc.core.model.StepParameter;

/** Steps declared the cucumber-java8 way: calls made by a class that implements a language interface. */
class LambdaStepsTest {

    private static final String FILE = "bookshop/CartSteps.java";

    private final StepDefinitionParser parser = new StepDefinitionParser();

    private StepCatalog parse(String source) {
        return parser.parseSource(FILE, source);
    }

    @Test
    void readsTheStepsAClassDeclaresWithLambdas() {
        StepCatalog catalog = parse("""
                package bookshop;

                import io.cucumber.java8.En;

                public class CartSteps implements En {

                    public CartSteps() {
                        Given("an empty cart", () -> { });
                        this.When("I add {int} copies of {string}", (Integer quantity, String title) -> { });
                    }
                }
                """);

        assertThat(catalog.problems()).isEmpty();
        assertThat(catalog.steps())
                .extracting(StepDefinition::keyword, StepDefinition::kind, StepDefinition::expression,
                        StepDefinition::declaringClass, StepDefinition::methodName, step -> step.location().line())
                .containsExactly(
                        tuple("Given", StepKind.GIVEN, "an empty cart", "bookshop.CartSteps", "", 8),
                        tuple("When", StepKind.WHEN, "I add {int} copies of {string}", "bookshop.CartSteps", "", 9));
        assertThat(catalog.steps().get(1).parameters())
                .extracting(StepParameter::name, StepParameter::javaType, StepParameter::expressionType)
                .containsExactly(tuple("quantity", "Integer", "int"), tuple("title", "String", "string"));
    }

    @Test
    void acceptsTheInterfaceOfAnyLanguageBehindAWildcardImportOrFullyQualified() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.*;

                class PanierSteps implements Fr {
                    PanierSteps() {
                        Quand("j'ajoute un livre", () -> { });
                    }
                }

                class WarenkorbSteps implements java.io.Serializable, io.cucumber.java8.De {
                    WarenkorbSteps() {
                        Gegebensei("ein leerer Warenkorb", () -> { });
                    }
                }
                """);

        assertThat(catalog.steps())
                .extracting(StepDefinition::keyword, StepDefinition::kind, StepDefinition::declaringClass)
                .containsExactly(
                        tuple("Quand", StepKind.WHEN, "PanierSteps"),
                        tuple("Gegebensei", StepKind.GIVEN, "WarenkorbSteps"));
    }

    @Test
    void doesNotTakeALookAlikeCallForAStep() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.*;
                import com.acme.workflow.En;

                class Workflow implements En {
                    Workflow() {
                        When("a project type of that name", () -> { });
                    }
                }

                class NoInterface {
                    NoInterface() {
                        When("no interface, so a method of the project", () -> { });
                    }
                }

                class CartSteps implements Fr {
                    CartSteps(Workflow other) {
                        other.Quand("a call made on another object", () -> { });
                        When("a keyword of another language", () -> { });
                        Quand("not the two arguments of a step");
                    }
                }

                class Catalog implements Shelf {
                    Catalog() {
                        ParameterType("genre", "novel|essay", (String value) -> value);
                    }
                }
                """);

        assertThat(catalog.steps()).isEmpty();
        assertThat(catalog.parameterTypes()).isEmpty();
        assertThat(catalog.problems()).isEmpty();
    }

    @Test
    void documentsALambdaStepWithTheBlockCommentAboveItsCall() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.En;

                class CartSteps implements En {
                    CartSteps() {
                        /*
                         * Puts copies of a book in the cart.
                         *
                         * @param quantity how many copies
                         */
                        When("I add {int} copies of {string}", (Integer quantity, String title) -> { });

                        /** Empties the cart. */
                        Given("an empty cart", () -> { });

                        // A line comment is not documentation.
                        Then("the cart is empty", () -> { });
                    }
                }
                """);

        assertThat(catalog.steps()).extracting(StepDefinition::description)
                .containsExactly("Puts copies of a book in the cart.", "Empties the cart.", "");
        assertThat(catalog.steps().get(0).parameters()).extracting(StepParameter::description)
                .containsExactly("how many copies", "");
    }

    @Test
    void readsAStepTextWrittenAsAConstant() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.En;

                class CartSteps implements En {
                    private static final String ADD = "I add a book";

                    CartSteps() {
                        When(ADD, () -> { });
                    }
                }
                """);

        assertThat(catalog.steps()).extracting(StepDefinition::expression).containsExactly("I add a book");
    }

    @Test
    void keepsWhatALambdaSaysOfItsParametersAndNoMore() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.En;

                class CartSteps implements En {
                    CartSteps() {
                        When("I add {int} books", quantity -> { });
                        When("I remove {int} books", (var quantity) -> { });
                        Given("these books:", (DataTable books) -> { });
                        Given("this review:", (String review) -> { });
                    }
                }
                """);

        assertThat(catalog.problems()).isEmpty();
        assertThat(catalog.steps())
                .extracting(step -> step.parameters().get(0))
                .extracting(StepParameter::name, StepParameter::javaType, StepParameter::source)
                .containsExactly(
                        tuple("quantity", "", StepParameter.Source.EXPRESSION),
                        tuple("quantity", "", StepParameter.Source.EXPRESSION),
                        tuple("books", "DataTable", StepParameter.Source.DATA_TABLE),
                        tuple("review", "String", StepParameter.Source.DOC_STRING));
    }

    @Test
    void listsAStepGivenAMethodReferenceWithTheParametersOfItsTextOnly() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.En;

                class CartSteps implements En {
                    CartSteps() {
                        When("I add {int} books", this::add);
                    }

                    private void add(Integer quantity, Object notAStepArgument) {
                    }
                }
                """);

        assertThat(catalog.problems()).isEmpty();
        assertThat(catalog.steps()).singleElement()
                .satisfies(step -> assertThat(step.parameters())
                        .extracting(StepParameter::name, StepParameter::javaType, StepParameter::expressionType)
                        .containsExactly(tuple("", "", "int")));
    }

    @Test
    void reportsALambdaStepItCannotReadOrThatDisagreesWithItsText() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.En;

                class CartSteps implements En {
                    CartSteps() {
                        When(Phrases.ADD, () -> { });
                        Then("the cart has {int} books", () -> { });
                    }
                }
                """);

        assertThat(catalog.steps()).extracting(StepDefinition::expression).containsExactly("the cart has {int} books");
        assertThat(catalog.problems()).containsExactly(
                new Problem(new SourceLocation(FILE, 5),
                        "Step expression of the When lambda cannot be read from the scanned sources, the step was skipped"),
                new Problem(new SourceLocation(FILE, 6),
                        "Step \"the cart has {int} books\" has 1 parameter(s) but the Then lambda takes 0"));
    }

    @Test
    void readsTheParameterTypesDeclaredWithALambda() {
        StepCatalog catalog = parse("""
                import io.cucumber.java8.LambdaGlue;

                class Types implements LambdaGlue {
                    Types() {
                        /* Color of a book cover. */
                        ParameterType("color", "red|blue", (String value) -> value);
                        ParameterType("size", Sizes.PATTERN, (String value) -> value);
                        ParameterType(Names.ISBN, "\\\\d{13}", (String value) -> value);
                        ParameterType("not the three arguments of a parameter type", "x");
                    }
                }
                """);

        assertThat(catalog.parameterTypes())
                .extracting(ParameterTypeDefinition::name, ParameterTypeDefinition::regex,
                        ParameterTypeDefinition::description, type -> type.location().line())
                .containsExactly(tuple("color", "red|blue", "Color of a book cover.", 6));
        assertThat(catalog.problems()).extracting(Problem::message).containsExactly(
                "Parameter type 'size' cannot be read from the scanned sources, it was ignored",
                "Parameter type 'Names.ISBN' cannot be read from the scanned sources, it was ignored");
    }
}