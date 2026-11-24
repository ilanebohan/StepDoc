# Architecture

Design background for StepDoc: how the pieces fit, why they are built this way, and how to
make the most common changes. This is not a file tour: read the code for that.

## The pipeline

```
.java sources --> parser --> StepCatalog (steps + problems) --+--> render --> Markdown / HTML
                                                              +--> lint ----> violations
```

Everything flows through the model. The parser is the only producer of a `StepCatalog`;
renderers and the linter only consume it and never look at source files.

| Module | Role | Depends on |
|---|---|---|
| `stepdoc-annotations` | The optional `@StepDoc` annotation, source-retained | nothing |
| `stepdoc-core` | Parser, model, renderers, linter | JavaParser only |
| `stepdoc-maven-plugin` | Reads settings, calls core, reports to Maven | core, Maven API |

Packages of `stepdoc-core` (under `com.exodius_software.stepdoc.core`):

| Package | Holds | Public entry points |
|---|---|---|
| `model` | Immutable records: the contract between the parser and its consumers | all of it |
| `parser` | Source scanning with JavaParser | `StepDefinitionParser` |
| `render` | Layout decisions and the output formats | `OutputFormat`, `RenderOptions`, `GroupBy`, `CatalogRenderer` |
| `lint` | Documentation rules | `CatalogLinter`, `LintRule`, `Violation` |
| `util` | Shared helpers | `EnumNames` (internal, public for technical reasons) |
| (root) | `DocumentationGenerator`: scan, then write one file per format | itself |

## Design decisions

**Source-only parsing.** StepDoc works on syntax trees, never on compiled classes, so it can
document a project that does not build and adds no dependency to the user's classpath.
What follows from it:

- A step annotation is recognised from the file's imports. Explicit imports and fully
  qualified names work for every Gherkin language; a wildcard import (`io.cucumber.java.en.*`)
  only resolves the five English keywords, because the content of a package is unknown.
- A step whose expression is a constant (`@When(Steps.ADD)`) cannot be resolved: it is skipped
  and reported as a `Problem`. String literals, text blocks and their concatenation are read.
- `@StepDoc` is read from the syntax tree, which is why it can be source-retained.
- StepDoc itself targets Java 17, but its parser accepts sources written for newer versions.

**Problems instead of exceptions.** A file that cannot be parsed, an expression that cannot be
read, a step and a method that disagree on parameters: each becomes a `Problem` in the catalog
and the scan continues. One broken file must never hide every other step. Each consumer then
decides what a problem means: a warning for `generate`, a violation for `check`.

**Deterministic output.** Files are visited in relative path order, steps keep their source
order, groups use insertion-ordered or enum-ordered maps, and no document contains a date or a
machine-specific path. Generated documentation is meant to be committed or published and
compared between builds.

**Javadoc first, `@StepDoc` second.** A team adopts StepDoc without touching its code: Javadoc
summaries and `@param` tags are the default documentation. `@StepDoc` only adds what Javadoc
cannot express (examples) and wins when both describe the same thing.

**One layout, several formats.** `CatalogLayout` owns every decision that does not depend on
the format: grouping, ordering, section titles, parameter type labels. A renderer only knows
syntax and escaping. Two formats cannot disagree on content.

**Empty string, never `null`.** Text fields of the model are never `null`; an empty string
means "not available". Collections are immutable copies.

**The compiler finds missing cases.** Switches on project enums (`LintRule`, `GroupBy`,
`StepKind`, `StepParameter.Source`) are exhaustive and have no `default`, so adding a constant
breaks compilation wherever it must be handled.

**One spelling for configuration values.** `EnumNames` maps enum constants to user-facing
names (`UNDOCUMENTED_STEP` is `undocumented-step`), ignores case, and produces the error
message that lists valid values.

**A thin plugin.** Mojos hold no logic: they read `@Parameter` fields, call core and translate
the result. `AbstractStepDocMojo` handles what every goal shares (source directory, `skip`,
error mapping). Core never depends on Maven, so another build tool can reuse it.

**A self-contained HTML page.** Styles and script are inlined and nothing is loaded from the
network: the file works offline and can be published as is.

## Recipes

Each recipe lists every place a change must touch. If you find one that is missing, add it here.

**Add a lint rule**

1. Add the constant to `LintRule`, with a Javadoc line saying what it reports.
2. The build now fails in `CatalogLinter.check`: implement the rule there as a small method.
3. Test it in `CatalogLinterTest`, and update the test that lists the valid rule ids.
4. Document it: rule table in `README.md`, Javadoc of `rules` in `CheckMojo`.

**Add an output format**

1. Write a package-private `CatalogRenderer` in `render`. Take sections and labels from
   `CatalogLayout`; escape every value that comes from the catalog.
2. Register it in `OutputFormat` with its file extension.
3. Test it like `MarkdownRendererTest`, including escaping, and update `OutputFormatTest`.
4. Document it: `formats` in `README.md` and in the Javadoc of `GenerateMojo`.

**Add a plugin setting**

1. Add a `@Parameter(property = "stepdoc.<name>", defaultValue = "...")` field to the mojo,
   or to `AbstractStepDocMojo` if every goal needs it. Its Javadoc is the user documentation.
2. Carry the value to core through an existing options type (`RenderOptions`, `OutputSettings`)
   rather than a new method parameter. Validate in core, with an `IllegalArgumentException`.
3. Test the behavior in core, and add the setting to the tables in `README.md`.

**Add information to the model**

1. Add the field to the record, with its `@param` line and its null check in the compact constructor.
2. Fill it in the parser, show it in **both** renderers, and decide whether the linter cares.
3. Update the test builders (`TestSteps`, the helpers of `CatalogLinterTest`).

**Change what the parser understands**

Start with a failing test in `StepDefinitionParserTest`: a few lines of Java source in a text
block, and assertions on the resulting catalog. Then change the parser.

## Known limitations

These are known and accepted for now. Do not "fix" one in passing: each needs its own issue,
because the obvious fix usually breaks a design decision above.

- Keywords from non-English packages (`io.cucumber.java.de.Wenn`) are kept as written but
  have the kind `StepKind.OTHER`: there is no keyword translation table.
- Non-English keywords behind a wildcard import are not detected.
- Expressions defined by a constant are skipped and reported.
- Steps written as lambdas (`cucumber-java8`) are not detected.
- Custom parameter types (`{color}`) are shown by name; their `@ParameterType` definition is not read.
- Steps are not matched against `.feature` files, so unused steps are not reported.
