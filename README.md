# Cucumber StepDoc

Living documentation for your Cucumber step definitions.

Large test suites end up with hundreds of steps nobody can find. StepDoc reads your
step definition sources and generates a searchable catalog: every step, its
description, and what each parameter expects, with examples.

> Status: early development, nothing usable yet.

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

## Build

Requires a JDK 17+.

```shell
./mvnw verify
```

## License

[MIT](LICENSE)
