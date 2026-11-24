package com.exodius_software.stepdoc.core.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.exodius_software.stepdoc.core.model.ExpressionSyntax;
import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepKind;
import com.exodius_software.stepdoc.core.model.StepParameter;

class StepDefinitionParserTest {

    private static final String FILE = "bookshop/CartSteps.java";

    private final StepDefinitionParser parser = new StepDefinitionParser();

    private StepCatalog parse(String source) {
        return parser.parseSource(FILE, source);
    }

    private StepDefinition onlyStep(String source) {
        StepCatalog catalog = parse(source);
        assertThat(catalog.steps()).hasSize(1);
        return catalog.steps().get(0);
    }

    @Nested
    class StepDetection {

        @Test
        void readsAnExplicitlyImportedStepWithItsLocation() {
            StepDefinition step = onlyStep("""
                    package bookshop;

                    import io.cucumber.java.en.When;

                    public class CartSteps {

                        @When("I add a book to the cart")
                        public void addBook() {
                        }
                    }
                    """);

            assertThat(step.keyword()).isEqualTo("When");
            assertThat(step.kind()).isEqualTo(StepKind.WHEN);
            assertThat(step.expression()).isEqualTo("I add a book to the cart");
            assertThat(step.declaringClass()).isEqualTo("bookshop.CartSteps");
            assertThat(step.methodName()).isEqualTo("addBook");
            assertThat(step.location()).isEqualTo(new SourceLocation(FILE, 7));
        }

        @Test
        void resolvesEnglishKeywordsThroughAWildcardImport() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.*;

                    class CartSteps {
                        @Given("an empty cart") void emptyCart() {}
                        @Then("the cart is empty") void cartIsEmpty() {}
                    }
                    """);

            assertThat(catalog.steps()).extracting(StepDefinition::kind)
                    .containsExactly(StepKind.GIVEN, StepKind.THEN);
        }

        @Test
        void ignoresHooksAndOtherAnnotationsEvenWithWildcardImports() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.*;
                    import io.cucumber.java.en.*;

                    class CartSteps {
                        @Before void resetCart() {}
                        @SuppressWarnings("unused") void helper() {}
                        @Deprecated @When("I empty the cart") void emptyCart() {}
                    }
                    """);

            assertThat(catalog.steps()).extracting(StepDefinition::methodName).containsExactly("emptyCart");
        }

        @Test
        void ignoresAnAnnotationExplicitlyImportedFromAnotherPackage() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.*;
                    import com.acme.workflow.When;

                    class CartSteps {
                        @When("order placed") void onOrderPlaced() {}
                    }
                    """);

            assertThat(catalog.steps()).isEmpty();
        }

        @Test
        void acceptsAFullyQualifiedStepAnnotation() {
            StepDefinition step = onlyStep("""
                    class OrderSteps {
                        @io.cucumber.java.en.Then("the order is confirmed") void confirmed() {}
                    }
                    """);

            assertThat(step.kind()).isEqualTo(StepKind.THEN);
        }

        @Test
        void classifiesANonEnglishKeywordAndKeepsItAsWritten() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.de.Wenn;

                    class CartSteps {
                        @Wenn("I add a book") void addBook() {}
                    }
                    """);

            assertThat(step.keyword()).isEqualTo("Wenn");
            assertThat(step.kind()).isEqualTo(StepKind.WHEN);
        }

        @Test
        void resolvesTheKeywordsOfEveryWildcardImportedLanguage() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.de.*;
                    import io.cucumber.java.en.*;

                    class CartSteps {
                        @Gegebensei("an empty cart") void emptyCart() {}
                        @When("I add a book") void addBook() {}
                        @Dann("the cart has one book") void oneBook() {}
                    }
                    """);

            assertThat(catalog.steps())
                    .extracting(StepDefinition::keyword, StepDefinition::kind)
                    .containsExactly(
                            tuple("Gegebensei", StepKind.GIVEN),
                            tuple("When", StepKind.WHEN),
                            tuple("Dann", StepKind.THEN));
        }

        @Test
        void doesNotGuessAStepBehindAWildcardImportOfAnotherLanguage() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.de.*;

                    class CartSteps {
                        @When("I add a book") void addBook() {}
                    }
                    """);

            assertThat(catalog.steps()).isEmpty();
        }

        @Test
        void listsANamedStepAnnotationUnknownToGherkinWithoutGuessingItsKind() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.en.Whenever;

                    class CartSteps {
                        @Whenever("I add a book") void addBook() {}
                    }
                    """);

            assertThat(step.keyword()).isEqualTo("Whenever");
            assertThat(step.kind()).isEqualTo(StepKind.OTHER);
        }

        @Test
        void producesOneDefinitionPerStepAnnotationSharingTheMethodDocumentation() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.*;

                    class CartSteps {
                        /** Puts a book in the cart. */
                        @Given("a book in the cart")
                        @When("I add a book")
                        void addBook() {}
                    }
                    """);

            assertThat(catalog.steps())
                    .extracting(StepDefinition::expression, StepDefinition::description)
                    .containsExactly(
                            tuple("a book in the cart", "Puts a book in the cart."),
                            tuple("I add a book", "Puts a book in the cart."));
        }
    }

    @Nested
    class Parameters {

        @Test
        void mapsCucumberExpressionTypesToMethodParametersByPosition() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        @When("I add {int} copies of {string} for {}")
                        void add(int quantity, String title, Object customer) {}
                    }
                    """);

            assertThat(step.syntax()).isEqualTo(ExpressionSyntax.CUCUMBER_EXPRESSION);
            assertThat(step.parameters())
                    .extracting(StepParameter::name, StepParameter::javaType,
                            StepParameter::source, StepParameter::expressionType)
                    .containsExactly(
                            tuple("quantity", "int", StepParameter.Source.EXPRESSION, "int"),
                            tuple("title", "String", StepParameter.Source.EXPRESSION, "string"),
                            tuple("customer", "Object", StepParameter.Source.EXPRESSION, ""));
        }

        @Test
        void doesNotTreatEscapedBracesAsAParameter() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.Then;

                    class CartSteps {
                        @Then("the total reads \\\\{price} as {int} euros")
                        void total(int euros) {}
                    }
                    """);

            StepDefinition step = catalog.steps().get(0);
            assertThat(step.expression()).isEqualTo("the total reads \\{price} as {int} euros");
            assertThat(step.parameters()).extracting(StepParameter::expressionType).containsExactly("int");
            assertThat(catalog.problems()).isEmpty();
        }

        @Test
        void countsOnlyTheCapturingGroupsOfARegularExpression() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.When;

                    class PaymentSteps {
                        @When("^I pay (\\\\d+) euros [(]tax included[)] by (?:card|cash) for (?<order>\\\\w+)$")
                        void pay(int amount, String order) {}
                    }
                    """);

            StepDefinition step = catalog.steps().get(0);
            assertThat(step.syntax()).isEqualTo(ExpressionSyntax.REGULAR_EXPRESSION);
            assertThat(step.parameters())
                    .extracting(StepParameter::name, StepParameter::expressionType)
                    .containsExactly(tuple("amount", ""), tuple("order", ""));
            assertThat(catalog.problems()).isEmpty();
        }

        @Test
        void recognisesATrailingDataTableOrDocString() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.Given;

                    class CatalogSteps {
                        @Given("these books in stock:") void books(DataTable books) {}
                        @Given("the review of {string}:") void review(String title, String review) {}
                    }
                    """);

            assertThat(catalog.steps())
                    .extracting(step -> step.parameters().get(step.parameters().size() - 1).source())
                    .containsExactly(StepParameter.Source.DATA_TABLE, StepParameter.Source.DOC_STRING);
            assertThat(catalog.problems()).isEmpty();
        }

        @Test
        void reportsAnArityMismatchButStillListsTheStep() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        @When("I add {int} books") void add() {}
                    }
                    """);

            assertThat(catalog.steps()).singleElement()
                    .satisfies(step -> assertThat(step.parameters())
                            .extracting(StepParameter::name, StepParameter::expressionType)
                            .containsExactly(tuple("", "int")));
            assertThat(catalog.problems()).containsExactly(new Problem(new SourceLocation(FILE, 4),
                    "Step \"I add {int} books\" has 1 parameter(s) but method add takes 0"));
        }

        @Test
        void readsAConcatenatedExpression() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        @When("I add " + "a book") void add() {}
                    }
                    """);

            assertThat(step.expression()).isEqualTo("I add a book");
        }

        @Test
        void skipsAStepWhoseExpressionIsAConstantAndSaysWhy() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        @When(Expressions.ADD_BOOK) void add() {}
                    }
                    """);

            assertThat(catalog.steps()).isEmpty();
            assertThat(catalog.problems()).singleElement()
                    .satisfies(problem -> assertThat(problem.message()).contains("add", "not a string literal"));
        }

        @Test
        void detectsTheSyntaxTheWayCucumberDoes() {
            assertThat(StepExpressions.syntaxOf("^a cart")).isEqualTo(ExpressionSyntax.REGULAR_EXPRESSION);
            assertThat(StepExpressions.syntaxOf("a cart$")).isEqualTo(ExpressionSyntax.REGULAR_EXPRESSION);
            assertThat(StepExpressions.syntaxOf("/a cart/")).isEqualTo(ExpressionSyntax.REGULAR_EXPRESSION);
            assertThat(StepExpressions.syntaxOf("/")).isEqualTo(ExpressionSyntax.CUCUMBER_EXPRESSION);
            assertThat(StepExpressions.syntaxOf("a cart of {int}/{int} books"))
                    .isEqualTo(ExpressionSyntax.CUCUMBER_EXPRESSION);
        }
    }

    @Nested
    class Documentation {

        @Test
        void takesDescriptionsFromJavadocAndMatchesParamTagsByName() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        /**
                         * Adds copies of a book to the cart,
                         * creating the cart if needed.
                         *
                         * <p>Relies on {@code Cart#add}, see {@link Cart the cart}.
                         *
                         * @param title book title as shown in the catalog
                         * @param quantity number of copies
                         */
                        @When("I add {int} copies of {string}")
                        void add(int quantity, String title) {}
                    }
                    """);

            assertThat(step.description()).isEqualTo(
                    "Adds copies of a book to the cart, creating the cart if needed.\n\n"
                            + "Relies on Cart#add, see the cart.");
            assertThat(step.parameters())
                    .extracting(StepParameter::name, StepParameter::description)
                    .containsExactly(
                            tuple("quantity", "number of copies"),
                            tuple("title", "book title as shown in the catalog"));
        }

        @Test
        void prefersStepDocOverJavadocAndFallsBackPerParameter() {
            StepDefinition step = onlyStep("""
                    import io.cucumber.java.en.When;
                    import com.exodius_software.stepdoc.annotations.StepDoc;

                    class CartSteps {
                        /**
                         * Javadoc description.
                         *
                         * @param quantity number of copies
                         * @param title title from the Javadoc
                         */
                        @StepDoc(value = "Adds copies of a book to the cart", params = {
                                @StepDoc.Param(description = "", examples = {"1", "12"}),
                                @StepDoc.Param(description = "Book title", examples = "Dune")
                        })
                        @When("I add {int} copies of {string}")
                        void add(int quantity, String title) {}
                    }
                    """);

            assertThat(step.description()).isEqualTo("Adds copies of a book to the cart");
            assertThat(step.parameters())
                    .extracting(StepParameter::description, StepParameter::examples)
                    .containsExactly(
                            tuple("number of copies", List.of("1", "12")),
                            tuple("Book title", List.of("Dune")));
        }

        @Test
        void reportsStepDocParametersTheMethodDoesNotHave() {
            StepCatalog catalog = parse("""
                    import io.cucumber.java.en.When;

                    class CartSteps {
                        @StepDoc(params = {
                                @StepDoc.Param(description = "Quantity"),
                                @StepDoc.Param(description = "Leftover")
                        })
                        @When("I add {int} books") void add(int quantity) {}
                    }
                    """);

            assertThat(catalog.problems()).extracting(Problem::message)
                    .containsExactly("@StepDoc documents 2 parameter(s) but method add takes 1");
        }
    }

    @Nested
    class Directories {

        @TempDir
        Path root;

        @Test
        void scansJavaFilesRecursivelyInRelativePathOrder() throws IOException {
            write("steps/order/OrderSteps.java", """
                    import io.cucumber.java.en.Then;
                    class OrderSteps { @Then("the order is confirmed") void confirmed() {} }
                    """);
            write("steps/cart/CartSteps.java", """
                    import io.cucumber.java.en.*;
                    class CartSteps {
                        @Given("an empty cart") void empty() {}
                        @When("I add a book") void add() {}
                    }
                    """);
            write("steps/cart/README.md", "@When(\"not java\")");

            StepCatalog catalog = parser.parseDirectory(root, StandardCharsets.UTF_8);

            assertThat(catalog.steps())
                    .extracting(step -> step.location().file(), StepDefinition::expression)
                    .containsExactly(
                            tuple("steps/cart/CartSteps.java", "an empty cart"),
                            tuple("steps/cart/CartSteps.java", "I add a book"),
                            tuple("steps/order/OrderSteps.java", "the order is confirmed"));
        }

        @Test
        void reportsUnreadableFilesWithoutStoppingTheScan() throws IOException {
            write("steps/Broken.java", "class Broken {");
            Files.write(root.resolve("steps/Legacy.java"),
                    "class Legacy { /* caf\u00e9 */ }".getBytes(StandardCharsets.ISO_8859_1));
            write("steps/CartSteps.java", """
                    import io.cucumber.java.en.When;
                    class CartSteps { @When("I add a book") void add() {} }
                    """);

            StepCatalog catalog = parser.parseDirectory(root, StandardCharsets.UTF_8);

            assertThat(catalog.steps()).extracting(StepDefinition::expression).containsExactly("I add a book");
            assertThat(catalog.problems()).extracting(problem -> problem.location().file())
                    .containsExactly("steps/Broken.java", "steps/Legacy.java");
            assertThat(catalog.problems().get(0).message()).startsWith("File could not be parsed");
            assertThat(catalog.problems().get(1).message()).isEqualTo("File is not valid UTF-8, it was skipped");
        }

        @Test
        void readsSourcesInTheRequestedEncoding() throws IOException {
            Files.write(root.resolve("CafeSteps.java"), """
                    import io.cucumber.java.en.When;
                    class CafeSteps { @When("I order a caf\u00e9") void order() {} }
                    """.getBytes(StandardCharsets.ISO_8859_1));

            StepCatalog catalog = parser.parseDirectory(root, StandardCharsets.ISO_8859_1);

            assertThat(catalog.steps()).extracting(StepDefinition::expression).containsExactly("I order a caf\u00e9");
            assertThat(catalog.problems()).isEmpty();
        }

        @Test
        void namesTheEncodingAFileIsNotValidIn() throws IOException {
            Files.write(root.resolve("CafeSteps.java"),
                    "class CafeSteps { /* caf\u00e9 */ }".getBytes(StandardCharsets.ISO_8859_1));

            StepCatalog catalog = parser.parseDirectory(root, StandardCharsets.US_ASCII);

            assertThat(catalog.problems()).extracting(Problem::message)
                    .containsExactly("File is not valid US-ASCII, it was skipped");
        }

        private void write(String relativePath, String content) throws IOException {
            Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }
    }
}
