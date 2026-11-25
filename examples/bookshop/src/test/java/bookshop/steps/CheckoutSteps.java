package bookshop.steps;

import io.cucumber.java.en.*;

public class CheckoutSteps {

    /**
     * Pays the current cart.
     *
     * @param method {@code card} or {@code cash}
     */
    @When("^I pay by (card|cash)$")
    public void pay(String method) {
    }

    /**
     * Checks the confirmation email.
     *
     * @param content expected email body
     */
    @Then("I receive this confirmation:")
    public void confirmation(String content) {
    }

    /** Deliberately broken: the expression has a parameter the method does not take. */
    @Then("my order number is {int}")
    public void orderNumber() {
    }

    /** Deliberately ambiguous: "I pay by card" matches this step and the first one of this class. */
    @When("I pay by card")
    public void payByCard() {
    }
}
