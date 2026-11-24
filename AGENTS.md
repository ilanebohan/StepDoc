# AGENTS.md

Instructions for AI coding agents working on StepDoc.
Humans start with [CONTRIBUTING.md](CONTRIBUTING.md), the source of truth for project conventions.

StepDoc reads Cucumber step definition sources (Java) and turns them into a documentation
catalog (Markdown, HTML) and a documentation linter. It ships as a Maven plugin.

This file is kept short on purpose: it only holds what you cannot infer from the code.
Details live in the guides listed at the end. Read the one that matches your task, not all of them.

## Commands

| Goal | Command |
|---|---|
| Everything the CI checks | `./mvnw verify` |
| One test class or method | `./mvnw -pl stepdoc-core test -Dtest=CatalogLinterTest#reportsAStepWithoutDescription` |
| Try the plugin by hand | `./mvnw install -DskipTests`, then `../../mvnw stepdoc:generate` in `examples/bookshop` |

On Windows, use `mvnw.cmd`. A task is done when `./mvnw verify` passes: it compiles with
warnings as errors, runs the unit tests, enforces coverage floors and builds the Javadoc.
In `examples/bookshop`, `stepdoc:check` fails on purpose: the example contains broken steps.

## Hard rules

Each of these is a design decision. Breaking one is a bug, even if the build is green.

1. **Sources only.** StepDoc never compiles, loads or runs the project it scans, and has no
   access to its classpath. Users must be able to document a project that does not build.
2. **Deterministic output.** Same sources, same bytes: no date, no absolute path, no unordered
   collection, no locale-dependent formatting in anything StepDoc generates.
3. **Scanned text is untrusted.** Everything read from the scanned sources is escaped by every
   renderer before it reaches a document.
4. **A bad input never stops a scan.** Report a `Problem` and keep going. Exceptions are for
   invalid configuration (`IllegalArgumentException`) and I/O errors only.
5. **No new dependency** without the maintainer agreeing in an issue first: StepDoc runs inside
   other people's builds.
6. **Never weaken a check to get a green build.** No `@SuppressWarnings`, no lowered coverage
   floor, no disabled or deleted test, no CI change, unless that is the task itself.
7. **English and fictional examples.** Code, comments, tests and docs are in English. Examples
   use the fictional bookshop domain, never a real company, product or proprietary code.

## How to work here

- **Stay in scope.** Do what the task asks. Report unrelated problems instead of fixing them.
- **Verify, do not assume.** Read the code and its tests before changing it, run the build after.
  Say what you ran, and what you could not run.
- **Ask when the answer changes the design.** Otherwise decide, and state your assumption.
- **Git is opt-in.** Do not create branches, commit, push or open pull requests unless asked.
  Never push to `main` or `develop`, never rewrite history that is already pushed.
- **The human is the author.** The person who submits your work is accountable for every line.
  Make their review easy: small diffs, reasons for your choices, a list of assumptions.
- **AI assistance is declared, once.** It goes in the "AI assistance" section of the pull
  request, truthfully: which tool, and what it did. Commits carry no AI co-author trailer and
  no "generated with" line.

## Guides

| When you | Read |
|---|---|
| analyze an issue or plan a change | [docs/ai/analysis.md](docs/ai/analysis.md) |
| write or change production code | [docs/ai/development.md](docs/ai/development.md) |
| write or update tests | [docs/ai/testing.md](docs/ai/testing.md) |
| name a branch, write a commit or a pull request | [docs/ai/git-workflow.md](docs/ai/git-workflow.md) |
| review a change | [docs/ai/review.md](docs/ai/review.md) |
| need the design background or a recipe for a common change | [docs/ai/architecture.md](docs/ai/architecture.md) |

If a rule in these files is wrong or outdated, fix it in the same pull request as the change
that made it so.
