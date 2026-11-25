# Changelog

Notable changes to StepDoc, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The version number covers what users rely on: the goals of the Maven plugin and their
settings, the `@StepDoc` annotation and the rule ids. Until 1.0.0 a minor version may still
break one of them; such a change is listed under "Changed" or "Removed". The classes of
`stepdoc-core` are not a stable API.

## [Unreleased]

### Added

- A step text or a `@StepDoc` text written as a constant is read, when the constant is declared
  in the scanned sources, in the same file or in another one.
- Parameter types declared with `@ParameterType` are documented: the catalog lists them, each
  `{type}` parameter shows what it matches, and `unused-step` matches them with their own
  regular expression instead of assuming they accept anything.
- Steps written as lambdas (`cucumber-java8`) are found in the classes that implement `En` or
  the interface of another language. The block comment above the call documents the step.
  Parameter types declared with a `ParameterType(...)` call are read too.
- Rule `undefined-step` reports the steps of feature files that no step definition implements.
  Like every rule it applies by default: leave it out of `rules` if your scenarios use steps
  defined outside the scanned sources.
- Rule `ambiguous-step` reports the steps of feature files that several step definitions
  match, which Cucumber refuses to run.

### Changed

- The problem reported for a step text that cannot be read now says "cannot be read from the
  scanned sources" instead of "is not a string literal".

## [0.1.0] - 2026-11-24

### Added

- `stepdoc:generate` writes a catalog of the Cucumber step definitions of a project, in
  Markdown and HTML, from the sources alone: nothing is compiled or run.
- `stepdoc:check` fails the build on undocumented steps and parameters, duplicate steps,
  steps that no feature file uses, and sources that cannot be read.
- `stepdoc:help` lists the goals and their settings.
- `@StepDoc` adds descriptions and example values that Javadoc cannot express.
- Step annotations are recognised in every Gherkin language.
- Sources are read in the encoding of the project (`project.build.sourceEncoding`), UTF-8 when
  it sets none; `encoding` overrides it.
