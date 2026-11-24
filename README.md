# Cucumber StepDoc

Living documentation for your Cucumber step definitions.

Large test suites end up with hundreds of steps nobody can find. StepDoc reads your
step definition sources and generates a searchable catalog: every step, its
description, and what each parameter expects, with examples.

> Status: early development, not published yet.

## Goals

- **Zero friction**: Javadoc is the default source of documentation. The `@StepDoc`
  annotation is optional, for what Javadoc cannot express (example values).
- **Deterministic**: same sources, same output. No runtime, no test execution, no AI.
- **Several outputs** from one model: Markdown, static HTML with search, Confluence.
- **Lint mode**: fail the build on undocumented steps, parameter mismatches,
  unused or duplicated steps.

## Modules

| Module | Role |
|---|---|
| `stepdoc-annotations` | Optional `@StepDoc` annotation, source-retained |
| `stepdoc-core` | Parser, documentation model, renderers, linter |
| `stepdoc-maven-plugin` | `stepdoc:generate` and `stepdoc:check` goals |

## Usage

```xml
<plugin>
    <groupId>com.exodius-software.stepdoc</groupId>
    <artifactId>stepdoc-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <title>Bookshop steps</title>
        <formats>html</formats>
    </configuration>
</plugin>
```

```shell
mvn stepdoc:generate
```

Every setting can also be passed on the command line, e.g. `-Dstepdoc.formats=markdown`.

| Setting | Property | Default | |
|---|---|---|---|
| `sourceDirectory` | `stepdoc.sourceDirectory` | `src/test/java` | Root scanned for step definitions |
| `outputDirectory` | `stepdoc.outputDirectory` | `target/stepdoc` | Where files are written |
| `fileName` | `stepdoc.fileName` | `steps` | File name without extension |
| `formats` | `stepdoc.formats` | `markdown,html` | `markdown`, `html` or both |
| `title` | `stepdoc.title` | `Step catalog` | Document title |
| `groupBy` | `stepdoc.groupBy` | `class` | `class` or `keyword` |
| `showSourceLocation` | `stepdoc.showSourceLocation` | `true` | File and line column |
| `showProblems` | `stepdoc.showProblems` | `true` | Section listing what could not be documented |
| `htmlStylesheet` | `stepdoc.htmlStylesheet` | | CSS file appended to the HTML page styles |
| `skip` | `stepdoc.skip` | `false` | Skips every StepDoc goal |

### Checking the documentation

`mvn stepdoc:check` (bound to `verify` when declared in `<executions>`) fails the build when
a step breaks one of the rules:

| Rule | Reported when |
|---|---|
| `undocumented-step` | A step has no description, neither Javadoc nor `@StepDoc` |
| `undocumented-parameter` | A step argument has no description |
| `duplicate-step` | Two methods share the same expression, which Cucumber refuses to run |
| `source-problem` | A file cannot be parsed, or a step and its method disagree on parameters |

| Setting | Property | Default | |
|---|---|---|---|
| `rules` | `stepdoc.rules` | all rules | Rule ids to apply, comma-separated |
| `failOnViolation` | `stepdoc.failOnViolation` | `true` | `false` only logs warnings |

`sourceDirectory` and `skip` work as for `generate`.

To try both goals on a fictional project, run `./mvnw install`, then
`../../mvnw stepdoc:generate stepdoc:check` from `examples/bookshop`.

## Build

Requires a JDK 17+.

```shell
./mvnw verify
```

## License

[MIT](LICENSE)
