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

- `stepdoc:generate` writes a catalog of the Cucumber step definitions of a project, in
  Markdown and HTML, from the sources alone: nothing is compiled or run.
- `stepdoc:check` fails the build on undocumented steps and parameters, duplicate steps,
  steps that no feature file uses, and sources that cannot be read.
- `stepdoc:help` lists the goals and their settings.
- `@StepDoc` adds descriptions and example values that Javadoc cannot express.
- Step annotations are recognised in every Gherkin language.
