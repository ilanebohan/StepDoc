package bookshop.steps;

import fr.bohanilane.stepdoc.annotations.StepDoc;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CartSteps {

    /** Starts the scenario with a cart containing nothing. */
    @Given("an empty cart")
    public void emptyCart() {
    }

    /**
     * Adds copies of a catalog book to the cart. The cart is created if needed.
     *
     * @param quantity number of copies
     * @param title    book title
     */
    @StepDoc(params = {
            @StepDoc.Param(description = "Number of copies, at least 1", examples = {"1", "3"}),
            @StepDoc.Param(description = "Book title, exactly as shown in the catalog", examples = {"Dune", "Neuromancer"})
    })
    @When("I add {int} copies of {string}")
    public void addCopies(int quantity, String title) {
    }

    /**
     * Fills the cart from a table with one row per book.
     *
     * @param books columns {@code title} and {@code quantity}
     */
    @Given("a cart with these books:")
    public void cartWithBooks(DataTable books) {
    }

    /** Checks the total, taxes included. */
    @Then("the cart total is {double} euros")
    public void cartTotal(double total) {
    }

    @Then("the cart is empty")
    public void cartIsEmpty() {
    }
}
