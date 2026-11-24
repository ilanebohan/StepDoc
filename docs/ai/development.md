# Writing code

How to change production code in StepDoc. General clean-code advice is not repeated here:
this guide covers what is specific to this project. Analyze first ([analysis.md](analysis.md)),
and check [architecture.md](architecture.md) for a recipe before you start.

## Before you write

- Read the class you change, its tests and its callers. Follow the pattern already in place,
  even if you would have chosen another one.
- Keep the change inside the task. If the code around it deserves a cleanup, say so in your
  report instead of doing it.
- If a small restructuring makes the change easier, do it first and keep it separate from the
  behavior change (its own commit): a reviewer must be able to tell "moved" from "changed".

## Conventions of this code base

**Language level.** Java 17, whatever JDK you run: records, sealed types, switch expressions,
pattern matching for `instanceof` and text blocks are fine; anything newer does not compile.

**Model.** Records only, immutable. Validate in the compact constructor
(`Objects.requireNonNull`, `List.copyOf`). Absent text is `""`, never `null`. `Optional` is a
return type, never a field or a parameter.

**Visibility.** Package-private by default. A type is `public` only when a build tool needs it
(see the entry points in [architecture.md](architecture.md)). Every public type is an API we
must keep stable: do not widen visibility to make something convenient.

**Helpers.** Stateless helpers are `final` classes with a private constructor and static methods.

**Enums.** Switch on project enums without `default`, so a new constant fails compilation where
it must be handled. Read enum values from configuration through `EnumNames`, never with `valueOf`.

**Errors.** Three cases, three mechanisms:

| Situation | Mechanism |
|---|---|
| Something wrong in the scanned sources | a `Problem` in the catalog; the scan continues |
| An invalid setting | `IllegalArgumentException` whose message names the setting and lists valid values |
| The file system fails | `IOException`, propagated to the caller |

Never catch an exception to ignore it, and never return `null` to signal a failure.

**Scanned text.** Anything that comes from the scanned sources (expressions, descriptions,
names, paths) is data. Renderers escape it with the existing helpers of each renderer; never
concatenate it raw into a document. A new renderer needs a test proving it.

**Messages.** Problems, violations and logs are short English sentences that say what is wrong
and where. Quote step expressions. A user must be able to fix the issue from the message alone.

**Vocabulary.** One word per concept, the one the code already uses: *step*, *expression*,
*parameter*, *catalog*, *section*. A *problem* comes from the parser, a *violation* from the linter.

**Comments.** Javadoc states the contract or the reason, in one or two sentences. Do not
paraphrase the code. Explain every non-obvious decision where it is made. No `TODO` without an
issue number.

**Formatting.** Follow `.editorconfig` and the surrounding code. Do not reformat lines you do
not change.

## The build is strict

- `-Xlint:all -Werror`: every compiler warning fails the build. Fix the cause; do not suppress it.
- Javadoc is built and linted: a broken `{@link}`, malformed HTML or an unknown `@param` fails.
- Maven Enforcer rejects diverging dependency versions.
- JaCoCo fails `verify` if core coverage drops below the floors (see [testing.md](testing.md)).

## Plugin code

Mojos stay thin: fields annotated with `@Parameter`, one call into core, translation of the
result into logs or a build failure. If you are writing an `if` about steps in a mojo, it
belongs in core, where it can be unit tested.

## Definition of done

- [ ] `./mvnw verify` passes, and you ran it yourself.
- [ ] Every behavior change has a test that fails without it.
- [ ] The user documentation is updated if users can see the change (setting, rule, output,
      message): `docs/user-guide.md` is the reference, `README.md` the short version.
- [ ] For a rendering or plugin change: you ran the plugin on `examples/bookshop` and looked at
      the generated files.
- [ ] These guides are updated if you changed a design decision, a recipe or a limitation.

## What to report

1. What you changed and why, most important first.
2. Choices that could have gone another way, and why you went this way.
3. Assumptions you made without confirmation.
4. What you left out on purpose.
5. What you verified (commands run, results) and what you did not.
