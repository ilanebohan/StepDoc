# Contributing to StepDoc

Thanks for your interest! Bug reports, ideas and pull requests are all welcome.

## Before you start

- **Bug or idea?** Open an [issue](https://github.com/ilanebohan/StepDoc/issues) first.
- **Bigger change?** Discuss it in an issue before coding, so your work is not wasted.
- **Security issue?** Do not open a public issue: use
  [private vulnerability reporting](https://github.com/ilanebohan/StepDoc/security/advisories/new).

## Set up

You need Java 17+ and Git. Maven comes with the project (`mvnw`).

```shell
git clone https://github.com/ilanebohan/StepDoc.git
cd StepDoc
./mvnw verify
```

`verify` runs exactly what the CI runs: compilation (warnings are errors), unit tests,
coverage floors, Javadoc and build rules. If it passes locally, the CI build will pass too.

| Module | Role |
|---|---|
| `stepdoc-annotations` | The optional `@StepDoc` annotation |
| `stepdoc-core` | Parser, documentation model, Markdown and HTML renderers, linter |
| `stepdoc-maven-plugin` | The `generate` and `check` goals |
| `examples/bookshop` | Fake test project to try the plugin by hand |

## Branches

- `main` only holds releases. Nobody pushes or merges there directly.
- `develop` is where work comes together. **Start from `develop` and open your pull request against `develop`.**

Name your branch `<github-user>/<type>/<issue>`:

| Type | For |
|---|---|
| `feature` | A new capability |
| `fix` | A bug fix |
| `tech` | Build, CI, refactoring, dependencies |
| `docs` | Documentation only |
| `test` | Tests only |

`<issue>` is the issue number or a short name in lowercase, e.g.
`octocat/feature/unused-steps` or `octocat/fix/42`. The CI rejects other names.

## Commits

Follow [Conventional Commits](https://www.conventionalcommits.org): `type(scope): summary`.

```
feat(core): detect steps never used in feature files
fix(render): escape pipes inside Markdown code spans
docs: explain the check goal in the README
```

Common types: `feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `build`, `chore`.
Keep the summary short and in the imperative; explain the *why* in the body if needed.

## Code

- **English everywhere**: code, comments, tests, examples, documentation.
- **Readable first**: clear names, small focused classes, comments that explain *why*.
- **No warnings**: the build treats them as errors.
- **Tests for every behavior change**: test what the code does, not how it does it.
  Core coverage must stay above 90% of lines and 80% of branches.
- **Javadoc on public API**, and follow `.editorconfig` (UTF-8, LF, 4 spaces).
- **No new dependency** without discussing it first: StepDoc runs inside other people's builds.

## Pull requests

1. Keep one topic per pull request.
2. Describe what changes and why, and link the issue (`Closes #42`).
3. Make sure the CI is green: **CI result** and **CodeQL** must pass.
4. Keep your branch up to date with `develop`.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
