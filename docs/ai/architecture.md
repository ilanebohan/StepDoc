# Architecture

Design background for StepDoc: how the pieces fit, why they are built this way, and how to
make the most common changes. This is not a file tour: read the code for that.

## The pipeline

```
.java sources ----> parser --> StepCatalog --+--> render --> Markdown / HTML
                                             +--> lint ----> violations
.feature files ---> usage ---> StepUsage ----------^
```

Everything flows through the model. The parser is the only producer of a `StepCatalog`;
renderers and the linter only consume it and never look at source files. Feature files are a
second input, read only to tell which steps are used and which are defined: `usage` compares
them with the catalog.

| Module | Role | Depends on |
|---|---|---|
| `stepdoc-annotations` | The optional `@StepDoc` annotation, source-retained | nothing |
| `stepdoc-core` | Parser, model, renderers, usage, linter | JavaParser, Gherkin, Cucumber Expressions |
| `stepdoc-maven-plugin` | Reads settings, calls core, reports to Maven | core, Maven API |

Packages of `stepdoc-core` (under `com.exodius_software.stepdoc.core`):

| Package | Holds | Public entry points |
|---|---|---|
| `model` | Immutable records: the contract between the parser and its consumers | all of it |
| `parser` | Source scanning with JavaParser | `StepDefinitionParser` |
| `render` | Layout decisions and the output formats | `OutputFormat`, `RenderOptions`, `GroupBy`, `CatalogRenderer` |
| `usage` | Feature file reading, matching of steps and step definitions | `StepUsageAnalyzer`, `StepUsage` |
| `lint` | Documentation rules | `CatalogLinter`, `LintRule`, `Violation` |
| `util` | Shared helpers | `EnumNames`, `FileTree`, `TextFiles`, `Whitespace` (internal, public for technical reasons) |
| (root) | `DocumentationGenerator` and `DocumentationChecker`: what a build tool calls | themselves |

## Design decisions

**Source-only parsing.** StepDoc works on syntax trees, never on compiled classes, so it can
document a project that does not build and adds no dependency to the user's classpath.
What follows from it:

- A step annotation is recognised from the file's imports. An annotation imported by name or
  written fully qualified is a step when its package is a cucumber-java language package
  (`io.cucumber.java.<language>`). Behind a wildcard import the content of the package is
  unknown, so only the keywords listed by `GherkinKeywords` are accepted.
- `GherkinKeywords` knows the step annotations of all Gherkin languages without cucumber-java
  on the classpath: it rebuilds their names from the dialects of the Gherkin library, with the
  naming rules of the cucumber-java code generator. `GherkinKeywordsTest` compares the result
  with every annotation of the real cucumber-java jar.
- A step text is read from string literals, text blocks, their concatenation, and constants
  declared in the scanned sources (`StringConstants`). Any other value cannot be known: the
  step is skipped and reported as a `Problem`.
- A step written as a lambda (`cucumber-java8`) is a call to a keyword method, made by a class
  that lists a language interface of `io.cucumber.java8` in its `implements` clause
  (`LambdaGlueTypes`). Those methods are named like the annotations of cucumber-java, which
  was checked once against the cucumber-java8 8.1.0 jar: there is no test for it, because
  cucumber-java8 is not a dependency. A lambda has no Javadoc, so the block comment above the
  call documents the step.
- `@StepDoc` is read from the syntax tree, which is why it can be source-retained.
- StepDoc itself targets Java 17, but its parser accepts sources written for newer versions.

**Problems instead of exceptions.** A file that cannot be parsed, an expression that cannot be
read, a step and a method that disagree on parameters: each becomes a `Problem` in the catalog
and the scan continues. One broken file must never hide every other step. Each consumer then
decides what a problem means: a warning for `generate`, a violation for `check`.

**Never call a used step unused.** Reporting an unused step is an accusation, and a wrong one
makes users turn the rule off. So `usage` answers the way Cucumber would: steps come from the
pickles of the Gherkin library (backgrounds included, outline placeholders replaced by example
values), expressions are matched by Cucumber's own expression library, and numbers are read in
the language of each feature file. When it cannot know, it stays silent: no feature file found
or a feature file that does not parse skips the rule with a warning, an expression that cannot
be matched becomes a `Problem`, and a custom parameter type is assumed to match anything.

The same comparison gives the reverse answer, the feature steps no definition implements,
under the same conditions. It has one blind spot the first does not: a definition the parser
never saw (other module, dependency, skipped with a `Problem`) makes its steps look undefined.
That cannot be detected from sources, so it is documented and the rule can be left out.

It also tells which feature steps several definitions match, which Cucumber refuses to run.
Silence is stricter there: a match that rests on an assumed parameter type does not count,
and definitions with the same text are left to `duplicate-step`.

**Deterministic output.** Source directories are visited in the order given and their files in
relative path order, steps keep their source order, groups use insertion-ordered or
enum-ordered maps, and no document contains a date or a machine-specific path. Generated
documentation is meant to be committed or published and compared between builds.

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
   A rule that needs the feature files takes its facts from `StepUsage`, never from the files.
3. Test it in `CatalogLinterTest`, and update the test that lists the valid rule ids.
4. Document it: rule tables in `docs/user-guide.md` and `README.md`, Javadoc of `rules` in `CheckMojo`.

**Add an output format**

1. Write a package-private `CatalogRenderer` in `render`. Take sections and labels from
   `CatalogLayout`; escape every value that comes from the catalog.
2. Register it in `OutputFormat` with its file extension.
3. Test it like `MarkdownRendererTest`, including escaping, and update `OutputFormatTest`.
4. Document it: `formats` in `docs/user-guide.md`, in `README.md` and in the Javadoc of `GenerateMojo`.

**Add a plugin setting**

1. Add a `@Parameter(property = "stepdoc.<name>", defaultValue = "...")` field to the mojo,
   or to `AbstractStepDocMojo` if every goal needs it. Its Javadoc is the user documentation.
2. Carry the value to core through an existing options type (`RenderOptions`, `OutputSettings`)
   rather than a new method parameter. Validate in core, with an `IllegalArgumentException`.
3. Test the behavior in core, and add the setting to the tables in `docs/user-guide.md` and `README.md`.

**Add information to the model**

1. Add the field to the record, with its `@param` line and its null check in the compact constructor.
2. Fill it in the parser, show it in **both** renderers, and decide whether the linter cares.
3. Update the test builders (`TestSteps`, the helpers of `CatalogLinterTest`).

**Change what the parser understands**

Start with a failing test in `StepDefinitionParserTest`: a few lines of Java source in a text
block, and assertions on the resulting catalog. Then change the parser.

**Upgrade Cucumber**

1. Raise `gherkin.version` and `cucumber-java.version` together in the parent `pom.xml`.
2. Run the tests. If `GherkinKeywordsTest` fails, cucumber-java names annotations differently
   or has a keyword the Gherkin library lacks: the failure lists them. Align `GherkinKeywords`
   with the code generator of cucumber-java (`GenerateI18n` in the cucumber-jvm repository).

## Known limitations

These are known and accepted for now. Do not "fix" one in passing: each needs its own issue,
because the obvious fix usually breaks a design decision above.

- A keyword that the bundled Gherkin library does not know (added by a newer Cucumber) has the
  kind `StepKind.OTHER` when imported by name, and is not detected behind a wildcard import.
- A step text or a parameter type defined by a constant declared outside the scanned sources is
  skipped and reported.
- Lambda steps are only found in a class that implements the cucumber-java8 interface itself:
  an inherited interface would need the type hierarchy. A method reference given in place of
  a lambda is not followed, so its parameters have no name.
- A custom parameter type (`{color}`) that is not declared in the scanned sources is shown by
  name, and the usage rules assume it matches anything.
