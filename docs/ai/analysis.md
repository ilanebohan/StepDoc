# Analyzing an issue

Do this before writing code. The goal is a short plan the human can check in two minutes,
so that a wrong direction costs a message, not a pull request.

Scale the effort to the change: a typo needs one sentence, a new feature needs the full analysis.

## Steps

1. **Restate the need** in one or two sentences, in your own words. If you cannot, the issue is
   not clear enough: ask.
2. **For a bug, reproduce it first.** Write the smallest step definition that shows it, as a
   failing test (see [testing.md](testing.md)). Find the cause, not just the symptom. A fix
   whose cause you cannot explain is not a fix: say so.
3. **Locate the change in the pipeline** (see [architecture.md](architecture.md)): parser, model,
   layout, a renderer, the linter, or the plugin. A model change reaches every stage.
   Check whether a recipe already describes it.
4. **Check the hard rules** of [AGENTS.md](../../AGENTS.md) and the known limitations. If the
   need can only be met by breaking one (for example, it requires the scanned project's
   classpath), stop and raise it: that is a design discussion, not an implementation detail.
5. **List the edge cases** that apply, using the checklist below.
6. **Name what users will see**: a new setting, a changed default, a different output, a new
   build failure. Changing what an existing configuration produces is a breaking change: call it out.
7. **Plan the tests**: which behaviors, in which test class.
8. **Size it.** More than one topic means more than one pull request: propose the split.

## Edge cases worth checking

Not every case applies to every change. Pick the relevant ones, and say which you ruled out.

- **Expressions**: Cucumber Expression or regular expression; anonymous `{}` parameter; custom
  parameter type; escaped braces (`\{`); optional text and alternatives (`book(s)`, `a/b`);
  non-capturing and named regex groups; expression built by concatenation or from a constant.
- **Method signature**: no parameter; trailing data table or doc string; parameter count that
  does not match the expression; several step annotations on one method.
- **Documentation**: no Javadoc; Javadoc without `@param`; `@StepDoc` alone; both, disagreeing;
  more `@StepDoc.Param` than parameters; multi-paragraph and inline-tag Javadoc.
- **Sources**: wildcard import; fully qualified annotation; non-English keyword; an annotation
  with a step-like name from another library; nested class; default package; file that does
  not parse; file that is not UTF-8; empty directory.
- **Feature files**: none found; a file that does not parse; another Gherkin language;
  background; scenario outline and its examples; a step used only through a custom parameter type.
- **Output**: text containing `|`, a backtick, `<`, `&` or quotes; empty catalog; one step;
  the same expression twice; Windows path separators.

## What to hand back

```
Need:            one or two sentences
Type:            fix | feature | tech | docs | test
Where:           pipeline stage(s) and main classes
Approach:        what you will change, in a few lines
Alternatives:    what you considered and why you dropped it
Edge cases:      the ones that apply, and how each is handled
Tests:           behaviors to cover, and where
Users will see:  settings, output or build changes, breaking or not
Open questions:  what you need answered, and the assumption you will take otherwise
```

## Ask or decide?

Ask when the answer changes the design or what users see: a default value, a new setting
versus a changed behavior, a public type, anything touching a hard rule.

Decide yourself, and say what you decided, for what the code already answers: naming, where
a helper goes, which existing pattern to follow.

One precise question is better than five vague ones, and much better than a hidden assumption.
