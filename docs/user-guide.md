# StepDoc user guide

Everything you need to use StepDoc on a project: what it is for, how to document your steps,
how to generate the catalog, how to check it in your build, and what each message means.

- [Why StepDoc](#why-stepdoc)
- [How it works](#how-it-works)
- [Install](#install)
- [Document your steps](#document-your-steps)
- [Generate the catalog](#generate-the-catalog)
- [Check the documentation](#check-the-documentation)
- [Settings shared by both goals](#settings-shared-by-both-goals)
- [Messages and how to fix them](#messages-and-how-to-fix-them)
- [Limits](#limits)

## Why StepDoc

A Cucumber test suite that grows ends up with hundreds of step definitions. Nobody knows them
all, so people write a new step instead of reusing one, guess what a parameter expects, and
leave behind steps that no scenario uses any more.

StepDoc gives the team two things:

- **A catalog**: every step, what it does and what each parameter expects, with examples,
  as a Markdown file and a searchable HTML page.
- **A check** that runs in the build and fails when a step is not documented, is defined
  twice, or is no longer used by any feature file.

## How it works

1. StepDoc reads the **source files** of your step definitions, as text.
2. It finds every method that carries a Cucumber step annotation (`@Given`, `@When`, `@Then`
   and their equivalents in every Gherkin language).
3. It takes the documentation of each step from its Javadoc, and from the optional `@StepDoc`
   annotation.
4. `stepdoc:generate` writes the catalog; `stepdoc:check` applies the documentation rules.

What this means for you:

- **Nothing is compiled or run.** StepDoc works on a project that does not build, and never
  starts your tests or your application.
- **Nothing leaves your machine.** No network call, no external service.
- **Same sources, same result.** The catalog contains no date and no machine-specific path:
  it only changes when your steps change, so you can commit it or compare two versions.

## Install

You need Java 17 or newer to run Maven, and Maven 3.9 or newer. The Java version your own
project targets does not matter.

StepDoc is not on Maven Central yet. Until the first release, install it locally:

```shell
git clone https://github.com/ilanebohan/StepDoc.git
cd StepDoc
./mvnw install
```

Then declare the plugin in the `pom.xml` of the module that contains your step definitions:

```xml
<plugin>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</plugin>
```

To use the `@StepDoc` annotation, also add its library. The annotation only exists in your
sources, so it adds nothing to what you ship:

```xml
<dependency>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Document your steps

### With Javadoc

You probably have nothing to change: StepDoc reads the Javadoc you already write.

```java
/**
 * Adds copies of a book to the cart. The cart is created if needed.
 *
 * @param quantity number of copies, at least 1
 * @param title    book title, exactly as shown in the catalog
 */
@When("I add {int} copies of {string}")
public void addCopies(int quantity, String title) { ... }
```

- The text before the tags is the **description of the step**.
- Each `@param` describes the **parameter of the same name**.

Keep the Javadoc simple. `{@code x}` and `{@link Type label}` become plain text, a blank line
or `<p>` separates paragraphs, and other HTML tags are shown as they are written.

### With `@StepDoc`

`@StepDoc` is optional. Use it for what Javadoc cannot express well: **example values**.

```java
@StepDoc(params = {
        @StepDoc.Param(description = "Number of copies, at least 1", examples = {"1", "3"}),
        @StepDoc.Param(description = "Book title, as shown in the catalog", examples = "Dune")
})
@When("I add {int} copies of {string}")
public void addCopies(int quantity, String title) { ... }
```

- `params` follows the **order of the method parameters**. A data table or doc string, always
  the last parameter, is documented the same way.
- `value` can hold the description of the step: `@StepDoc("Adds copies of a book")`.
- When Javadoc and `@StepDoc` both describe something, `@StepDoc` wins. A parameter whose
  `@StepDoc` description is empty (`description = ""`) keeps its Javadoc description, so you
  can add examples without repeating the text.

### What StepDoc recognises

| In your code | In the catalog |
|---|---|
| A step annotation of any Gherkin language, imported by name, through `.*` or written fully qualified | One entry, under its keyword |
| Several step annotations on one method | One entry per annotation, sharing the documentation |
| A Cucumber Expression: `I add {int} copies of {string}` | Each parameter with its type: `{int}`, `{string}`, `{}`, or your own `{color}` |
| A regular expression: `^I pay by (card\|cash)$` | Each capturing group, with the Java type of the method parameter |
| A data table or a doc string as last parameter | A parameter marked `data table` or `doc string` |

Hooks (`@Before`, `@After`) and annotations of other libraries are ignored.

### Tips for useful descriptions

- Say what the step does **for the person writing a scenario**, not how the code does it.
- For a parameter, say what values are accepted: a unit, a format, a list of choices.
- Give examples for anything that is not obvious. A tester copies an example faster than
  they read a sentence.

## Generate the catalog

```shell
mvn stepdoc:generate
```

This writes `target/stepdoc/steps.md` and `target/stepdoc/steps.html`. A step looks like this:

| Keyword | Step | Description | Parameters | Source |
| --- | --- | --- | --- | --- |
| When | `I add {int} copies of {string}` | Adds copies of a catalog book to the cart. The cart is created if needed. | **quantity** `{int}` Number of copies, at least 1 (e.g. `1`, `3`)<br>**title** `{string}` Book title, exactly as shown in the catalog (e.g. `Dune`, `Neuromancer`) | `bookshop/steps/CartSteps.java:26` |

- The **Markdown** file uses plain tables that GitHub and GitLab display as they are.
- The **HTML** page is a single file with no external resource: open it locally, attach it to
  a build, or publish it anywhere. It has a filter box and follows the light or dark theme of
  the reader.
- When StepDoc could not document something, such as a file it could not read, both list it in
  a **Problems** section at the end. `generate` never fails the build for that: failing is
  the job of `check`.

| Setting | Default | |
|---|---|---|
| `formats` | `markdown,html` | What to write: `markdown`, `html` or both |
| `title` | `Step catalog` | Title of the document |
| `groupBy` | `class` | `class`: one section per step class, steps ordered Given, When, Then.<br>`keyword`: one section per keyword, steps in source order |
| `outputDirectory` | `target/stepdoc` | Where the files are written |
| `fileName` | `steps` | File name, without extension |
| `showSourceLocation` | `true` | Shows the file and line of each step |
| `showProblems` | `true` | Shows the Problems section |
| `htmlStylesheet` | | A CSS file of yours, added after the default styles of the HTML page |

Set them in the plugin configuration, or on the command line with `-Dstepdoc.<setting>`:

```xml
<plugin>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <title>Bookshop steps</title>
        <formats>html</formats>
        <groupBy>keyword</groupBy>
    </configuration>
</plugin>
```

```shell
mvn stepdoc:generate -Dstepdoc.formats=markdown -Dstepdoc.showSourceLocation=false
```

## Check the documentation

```shell
mvn stepdoc:check
```

`check` prints one line per issue, with the rule, the file and the line, then fails the build.
On the example project of this repository:

```
[ERROR] [undocumented-parameter] bookshop/steps/CartSteps.java:40 Parameter 'total' of step "the cart total is {double} euros" has no description
[ERROR] [undocumented-step] bookshop/steps/CartSteps.java:44 Step "the cart is empty" has no description
[ERROR] [unused-step] bookshop/steps/CartSteps.java:44 Step "the cart is empty" is not used by any feature file
[ERROR] [unused-step] bookshop/steps/CheckoutSteps.java:26 Step "my order number is {int}" is not used by any feature file
[ERROR] [source-problem] bookshop/steps/CheckoutSteps.java:26 Step "my order number is {int}" has 1 parameter(s) but method orderNumber takes 0
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal ... StepDoc: 5 violation(s), 8 step(s) checked against 5 rule(s)
```

### Rules

| Rule | Reported when | How to fix |
|---|---|---|
| `undocumented-step` | A step has no description | Add a Javadoc summary, or `@StepDoc("...")` |
| `undocumented-parameter` | A parameter has no description, data tables and doc strings included | Add its `@param`, or a `@StepDoc.Param` |
| `duplicate-step` | Two methods have the same step text. Cucumber refuses to run such steps, whatever their keywords | Keep one of the two |
| `unused-step` | No `.feature` file uses the step | Delete the step, or write the scenario it was made for |
| `source-problem` | StepDoc could not read or understand something (see [Messages](#messages-and-how-to-fix-them)) | Depends on the message |

### Settings

| Setting | Default | |
|---|---|---|
| `rules` | all | Rules to apply, comma-separated. Empty means all of them, including rules added by future versions |
| `featureDirectories` | `src/test/resources` | Directories that hold your `.feature` files, comma-separated, searched recursively. Only used by `unused-step` |
| `failOnViolation` | `true` | `false` prints the issues as warnings and lets the build pass |

### How unused steps are found

StepDoc reads your feature files and matches every step of every scenario against your step
definitions, the way Cucumber does when it runs them:

- feature files in any Gherkin language (`# language: de`);
- steps of a `Background`, and steps of a `Scenario Outline` with the values of each example row;
- Cucumber Expressions and regular expressions.

It prefers saying nothing to being wrong:

- If it finds **no feature file**, or **cannot parse one**, the rule is skipped and a warning
  says why. Check `featureDirectories` if your features are not in `src/test/resources`.
- A step that uses a **parameter type of your own** (`{color}`) is considered used as soon as
  the rest of its text matches a scenario step, because StepDoc does not know which values the
  type accepts.

### Run it on every build

Bind the goals to your build, so the catalog is always up to date and the rules always hold:

```xml
<executions>
    <execution>
        <goals>
            <goal>generate</goal>
            <goal>check</goal>
        </goals>
    </execution>
</executions>
```

`generate` then runs before the tests are compiled and `check` during `mvn verify`.

### Adopt it step by step

On an existing project, the first `check` usually reports a lot. A way to get there:

1. Start with `failOnViolation` set to `false`: the build passes and you see the list.
2. Turn on the cheap rules first, for example `<rules>duplicate-step,source-problem</rules>`,
   and let them fail the build.
3. Add `undocumented-step`, then `undocumented-parameter`, as the team documents its steps.
4. Finish with `unused-step`, once you have agreed on what to do with unused steps.

## Settings shared by both goals

| Setting | Default | |
|---|---|---|
| `sourceDirectory` | `src/test/java` | Where your step definitions are, searched recursively |
| `skip` | `false` | Skips StepDoc, for example with `-Dstepdoc.skip` |

In a multi-module project, declare the plugin in each module that holds step definitions:
StepDoc documents the sources of the module it runs in, not the steps that come from a
dependency. If the feature files live in another module, point `featureDirectories` at it,
for example `../acceptance-tests/src/test/resources`.

If `sourceDirectory` does not exist, StepDoc says so and does nothing.

## Messages and how to fix them

Problems found in your sources appear in the Problems section of the catalog, as warnings of
`generate`, and as `source-problem` issues of `check`. The last two of this table only come
from `check`, the only goal that reads feature files.

| Message | Meaning | What to do |
|---|---|---|
| `File could not be parsed, it was skipped: ...` | The Java file has a syntax error | Fix the file; its steps are missing from the catalog until then |
| `File is not valid UTF-8, it was skipped` | StepDoc reads sources as UTF-8 | Convert the file to UTF-8 |
| `Step expression of method ... is not a string literal, the step was skipped` | The step text is a constant (`@When(Steps.ADD)`), which StepDoc cannot read without compiling | Write the text in the annotation |
| `Step "..." has 1 parameter(s) but method ... takes 0` | The step text and the method do not have the same number of parameters. Cucumber will fail on this step | Fix the expression or the method |
| `@StepDoc documents 2 parameter(s) but method ... takes 1` | `@StepDoc` describes more parameters than the method has | Remove the extra `@StepDoc.Param` |
| `Feature file could not be parsed: ...` | A `.feature` file is not valid Gherkin | Fix the file; `unused-step` is skipped until then |
| `Step "..." cannot be matched against feature files: ...` | The step text is not a valid Cucumber Expression or regular expression | Fix the expression; the message gives the column |

Warnings of `check`:

| Message | What to do |
|---|---|
| `Rule unused-step was skipped: no .feature file found in [...]` | Set `featureDirectories`, or remove `unused-step` from `rules` if this module has no feature file |
| `Rule unused-step was skipped: some feature files could not be parsed` | Fix the files reported just above as `source-problem` |

Configuration errors stop the build at once and list the accepted values, for example
`Unknown format 'pdf', expected one of: markdown, html`.

## Limits

- **Constants as step text** (`@When(Steps.ADD)`) are not read: the step is reported and skipped.
- **Steps written as lambdas** (`cucumber-java8`) are not detected.
- **Your own parameter types** (`{color}`) are shown by name, without the values they accept.
- **Keywords added by a Cucumber newer than StepDoc** are not classified as Given, When or
  Then when imported by name, and not detected behind a `.*` import, until StepDoc is updated.
- **Steps that no step definition implements** are not reported: Cucumber already tells you
  when it runs the scenario.
- **Only Java sources** are read: step definitions written in Kotlin, Scala or Groovy are not
  supported.
