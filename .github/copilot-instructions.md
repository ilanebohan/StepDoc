# Copilot instructions

The instructions for AI agents on this repository live in [`AGENTS.md`](../AGENTS.md) and in
the guides of [`docs/ai/`](../docs/ai). Follow them. Edit those files, not this one.

This file exists for the Copilot features that do not read `AGENTS.md`, such as code review.
It repeats only what a review must never miss.

## When reviewing a pull request

Apply [`docs/ai/review.md`](../docs/ai/review.md). Treat each of these as blocking:

- StepDoc compiles, loads or runs the project it scans, or needs its classpath. It must work
  on sources only.
- Generated output is not deterministic: a date, an absolute path, an unordered collection.
- Text read from scanned sources reaches a Markdown or HTML document without being escaped.
- A bad input stops a scan instead of being reported as a `Problem`.
- A dependency is added.
- A check is weakened: `@SuppressWarnings`, a lowered coverage floor, a disabled or deleted
  test, a workflow edited to pass.
- A behavior change comes without a test that would fail without it.
- A user-visible change is missing from `docs/user-guide.md` or `README.md`.

Report findings most severe first, with the file, the line, the reason and a suggestion.
Do not comment on style that the build does not enforce.
