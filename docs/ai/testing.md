# Writing tests

A test is worth having only if it can fail for a good reason. A few tests that protect real
behavior beat many that prove nothing: weak tests freeze refactoring and give false confidence.

## Before writing a test, ask

1. **Which bug does it catch?** If you cannot say it in one sentence, do not write it.
2. **Would it fail if the code were broken?** Imagine a condition inverted, a `>` turned into
   `>=`, a call removed. If the test stays green, it is empty: strengthen it or drop it.
3. **Would it survive a refactoring?** If renaming a private method or reordering internal
   calls breaks it, it tests the implementation: rewrite it against observable behavior.
4. **Can a reader tell in ten seconds** what is checked and why a failure happened?

If nothing useful can be tested (a record, a getter, pure delegation), say so and write nothing.
Never write a test to raise a coverage number.

## How tests are built here

The stack is JUnit Jupiter and AssertJ. There is no mocking library and none is needed: core is
made of pure functions over text, so tests use real objects.

| To test | Do this | Example |
|---|---|---|
| The parser | Give Java source in a text block to `parser.parseSource(...)`, assert on the catalog | `StepDefinitionParserTest` |
| A renderer or the layout | Build a catalog with the `TestSteps` builders, assert on the output | `MarkdownRendererTest` |
| The linter | Build a catalog, assert on the violations | `CatalogLinterTest` |
| Step usage | Write feature files in a `@TempDir`, parse step source, assert on the `StepUsage` | `StepUsageAnalyzerTest` |
| Anything touching files | Use `@TempDir`, never a real path | `DocumentationGeneratorTest` |

Tests go through the public entry point of a package (see [architecture.md](architecture.md)),
not through its package-private helpers, unless a helper encodes a rule worth pinning on its own.

The Maven plugin has no unit tests: mojos hold no logic, and the CI runs the real plugin on
`examples/bookshop`. Keep it that way by putting logic in core.

## Style

- **Name** = the behavior, as a camelCase sentence: `reportsAnArityMismatchButStillListsTheStep`.
  A name that is hard to write usually means the test does too much.
- **One behavior per test**, grouped with `@Nested` classes when a test class grows.
- **Arrange, act, assert**, separated by blank lines. No loop, no `if`, no computed expectation:
  expected values are written out, never derived with the logic under test.
- **Minimal data.** Only the values that matter for the behavior are visible. Use the fictional
  bookshop domain (cart, book, order, payment), in English.
- **Precise assertions.** `containsExactly` over `contains`, a value over `isNotNull`. Assert the
  exact text when the text is the contract (problem and violation messages, rendered rows), but
  do not pin unrelated details.
- **Readable over DRY.** Some repetition is fine if the test reads on its own. Share setup only
  through clearly named builders.
- **Independent and deterministic.** No shared mutable state, no order between tests, no
  network, no sleep, no dependency on the machine's locale, path separator or clock.

One trap: Java source inside a text block is escaped twice. A regex `\d+` in the scanned
source is written `\\\\d+` in the test, and double quotes need no escaping.

## What deserves a test in StepDoc

- Every rule of the parser: what counts as a step, how parameters are read, where
  documentation comes from, and each kind of `Problem`.
- Every layout decision and every lint rule, including the "does not report" side.
- Escaping in every renderer, with hostile input (`|`, backticks, `<script>`, quotes).
- Every error message a user can act on.

The edge case checklist in [analysis.md](analysis.md) is a good source of cases.

## Changing existing tests

- **Never weaken a test to make it pass**: no removed assertion, loosened matcher or `@Disabled`.
  First decide whether the behavior changed on purpose (update the test) or by accident (you
  found a regression: fix the code).
- **Bug fix**: write the failing test first, watch it fail, then fix.
- Touch only the tests your change affects. Do not restyle the others.

## Coverage

`./mvnw verify` fails if `stepdoc-core` drops below 90% of lines or 80% of branches. The report
is in `stepdoc-core/target/site/jacoco/index.html`. Use it to find untested logic, then decide
case by case whether it deserves a test. The floors are a safety net, not a target.

## Check your tests

- Run them: `./mvnw -pl stepdoc-core test -Dtest=YourTest`, then `./mvnw verify`.
- For each important test, break the code on purpose, watch the test fail, and restore the
  code (`git diff` must show only your intended change). If you cannot run it, reason through
  it and say so.

## What to report

Which behaviors are covered, what you chose not to test and why, the result of the run, and
anything suspicious you found in production code while writing the tests.
