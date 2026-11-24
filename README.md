# StepDoc

[![CI](https://github.com/ilanebohan/StepDoc/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/ilanebohan/StepDoc/actions/workflows/ci.yml)

StepDoc turns your Cucumber step definitions into a searchable catalog, and checks
that every step is documented.

> Early development: not on Maven Central yet.

New here? The [user guide](docs/user-guide.md) covers everything, step by step.

## How it works

1. StepDoc reads your step definition sources (`src/test/java` by default).
   Nothing is compiled and no test is run. Step annotations of every Gherkin language are
   recognised (`io.cucumber.java.en`, `io.cucumber.java.de`, ...).
2. Each step is described by its Javadoc. `@StepDoc` is optional, for example values.
3. `stepdoc:generate` writes the catalog in Markdown and HTML.
   `stepdoc:check` fails the build when a step is not documented, or not used by any feature file.

```java
/**
 * Adds copies of a book to the cart.
 *
 * @param quantity number of copies
 * @param title    book title, as shown in the catalog
 */
@StepDoc(params = {
        @StepDoc.Param(description = "Number of copies", examples = {"1", "3"}),
        @StepDoc.Param(description = "Book title", examples = "Dune")
})
@When("I add {int} copies of {string}")
public void addCopies(int quantity, String title) { ... }
```

becomes

| Keyword | Step | Description | Parameters |
| --- | --- | --- | --- |
| When | `I add {int} copies of {string}` | Adds copies of a book to the cart. | **quantity** `{int}` Number of copies (e.g. `1`, `3`)<br>**title** `{string}` Book title (e.g. `Dune`) |

The HTML page has the same content, a search box and a dark mode.

## Installation

Requires Java 17+ and Maven 3.9+.

Until the first release, install StepDoc locally:

```shell
git clone https://github.com/ilanebohan/StepDoc.git
cd StepDoc
./mvnw install
```

Then add the plugin to the `pom.xml` of your test project:

```xml
<plugin>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</plugin>
```

To use `@StepDoc`, add the annotations too. It only exists in your sources, so the `test` scope is enough:

```xml
<dependency>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Usage

```shell
mvn stepdoc:generate   # writes target/stepdoc/steps.md and steps.html
mvn stepdoc:check      # fails the build if a step is undocumented, duplicated or unused
```

To run both on every build, add an execution to the plugin:

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

## Configuration

Everything has a default. Set what you need in `<configuration>`, or on the command line
with `-Dstepdoc.<setting>=<value>`, e.g. `-Dstepdoc.formats=html`.

The [user guide](docs/user-guide.md) explains each setting, each rule and each message in detail.

**Both goals**

| Setting | Default | |
|---|---|---|
| `sourceDirectory` | `src/test/java` | Where the step definitions are |
| `skip` | `false` | Skips StepDoc |

**`generate`**

| Setting | Default | |
|---|---|---|
| `formats` | `markdown,html` | `markdown`, `html` or both |
| `title` | `Step catalog` | Document title |
| `groupBy` | `class` | One section per `class`, or per `keyword` (Given, When, Then...) |
| `outputDirectory` | `target/stepdoc` | Where files are written |
| `fileName` | `steps` | File name, without extension |
| `showSourceLocation` | `true` | Adds the file and line of each step |
| `showProblems` | `true` | Lists what could not be documented |
| `htmlStylesheet` | | CSS file added to the HTML page |

**`check`**

| Setting | Default | |
|---|---|---|
| `rules` | all | Rules to apply, comma-separated (see below) |
| `featureDirectories` | `src/test/resources` | Where the `.feature` files are, for the `unused-step` rule. Several directories can be given, comma-separated; they are searched recursively |
| `failOnViolation` | `true` | `false` only prints warnings |

| Rule | Fails when |
|---|---|
| `undocumented-step` | A step has no description |
| `undocumented-parameter` | A parameter has no description |
| `duplicate-step` | Two steps have the same text, which Cucumber refuses to run |
| `unused-step` | No `.feature` file uses the step |
| `source-problem` | A file cannot be read, or a step and its method disagree on parameters |

`unused-step` matches the steps of your feature files against your step definitions the way
Cucumber does: any Gherkin language, scenario outlines with their example values, Cucumber
Expressions and regular expressions. If it finds no feature file, or cannot parse one, it
prints a warning and reports nothing rather than calling used steps unused.

## Try it

`examples/bookshop` is a small fake test project. After `./mvnw install`, run from that folder:

```shell
../../mvnw stepdoc:generate stepdoc:check
```

`check` fails on purpose: the example contains undocumented, unused and broken steps.

## Contributing

Contributions are welcome, see [CONTRIBUTING.md](CONTRIBUTING.md). Changes are listed in the
[changelog](CHANGELOG.md), and security issues go through the [security policy](SECURITY.md).

## License

[MIT](LICENSE)
