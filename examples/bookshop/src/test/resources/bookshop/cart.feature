Feature: Cart

  Background:
    Given an empty cart

  Scenario: Adding books
    When I add 2 copies of "Dune"
    Then the cart total is 19.98 euros

  Scenario: Paying for a prepared cart
    Given a cart with these books:
      | title       | quantity |
      | Dune        | 1        |
      | Neuromancer | 2        |
    When I pay by card
    Then I receive this confirmation:
      """
      Thank you for your order.
      """

  Scenario: Using a coupon
    When I apply the coupon "WELCOME"
    Then the cart total is 9.99 euros
