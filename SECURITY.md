# Security policy

## Supported versions

StepDoc is in early development: security fixes go into the latest release only.

## Reporting a vulnerability

Do not open a public issue. Report it privately through
[GitHub private vulnerability reporting](https://github.com/ilanebohan/StepDoc/security/advisories/new).

Say what you can about:

- the StepDoc version and how you run it;
- the input that triggers the problem (a step definition, a feature file, a setting);
- what happens, and what you expected.

The maintainer aims to answer within a week. When a fix is released, the advisory is
published and credits you, unless you prefer otherwise.

## What to look for

StepDoc reads source files and writes documentation. It never compiles or runs the scanned
project and makes no network call. A security issue is therefore most likely one of these:

- text from a scanned file that reaches the generated HTML or Markdown without being escaped;
- a file read or written outside the configured directories;
- an input that makes the build hang or run out of memory.
