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

## Using AI tools

AI assistants are welcome here. Use them to explore the code, write, test or review, the way
you would use any other tool. One principle holds in every case: **you own what you submit.**

- **You are the author.** Whatever produced the first draft, you have read every line, you
  understand it, and you can explain and defend it in review. "The AI wrote it" is never an answer.
- **You verify.** Run `./mvnw verify`, read your own diff, and check that the tests really
  test something. Generated code often looks right and is wrong.
- **You answer for the license.** Only submit code you have the right to contribute under the
  [MIT License](LICENSE). Do not paste code that an assistant reproduced from another project,
  and do not give it proprietary code to adapt.
- **You respect reviewers' time.** Keep pull requests small and focused. Pull requests, issues
  or comments that were generated in bulk and not reviewed by their author will be closed.
- **You speak for yourself.** Using an assistant to polish your English is fine. Letting it
  argue with reviewers in your place is not.
- **You say so.** Declare AI assistance in the "AI assistance" section of the pull request:
  which tools, and what they did (code, tests, documentation, review). "None" is a valid
  answer; leaving the section empty or untrue is not.

The declaration is not a mark against your work: it tells reviewers where to look harder.
It lives in the pull request only. Commits carry no AI co-author trailer, because you are the
author and you are accountable for the change either way.

### Context for AI agents

The repository ships its own instructions for coding agents, so that they follow the same
rules as everyone else:

| File | Content |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Entry point: commands, hard rules, how to work here. Read by most agents. |
| [`docs/ai/`](docs/ai) | One guide per activity: analysis, development, testing, Git workflow, review, architecture. |
| `CLAUDE.md`, `.github/copilot-instructions.md` | Thin adapters for tools that look for their own file. |

If you use an agent, make sure it reads them, for example by starting with "analyze this issue
following `docs/ai/analysis.md`". They are useful to human contributors too:
[`docs/ai/architecture.md`](docs/ai/architecture.md) explains the design decisions.
If one of these files is wrong or outdated, fix it in your pull request.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
