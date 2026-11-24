# Reviewing a change

How to review a pull request or a diff on StepDoc, whether it was written by a human, by an
agent, or by you. The standard is the same in all three cases.

## Method

1. **Read the need first**: the issue and the pull request description. You review whether the
   change meets the need, not only whether the code looks fine.
2. **Read the whole diff**, then the code around it. A change can be correct in isolation and
   wrong in context.
3. **Verify instead of trusting.** When you can, check out the branch, run `./mvnw verify`,
   and try the plugin on `examples/bookshop` for a rendering change. Check that the description
   matches what the diff really does.
4. **Review the tests as code.** Apply the four questions of [testing.md](testing.md): a test
   that cannot fail is a finding.

## What to look for, by severity

**Blocking**: must be fixed before merging.

- A hard rule of [AGENTS.md](../../AGENTS.md) is broken: classpath or execution of the scanned
  project, non-deterministic output, unescaped scanned text in a document, a scan stopped by a
  bad input, a new dependency, a weakened check.
- The behavior is wrong, or an edge case from [analysis.md](analysis.md) that applies is not handled.
- A behavior change has no test, or its test would pass without the change.
- Existing behavior or a public type changes without being announced as breaking.
- Code that the contributor may not have the right to submit under the MIT License.

**Should fix**: merge only with a good reason.

- The change does more than its topic.
- Logic sits in the wrong place: in a mojo instead of core, in one renderer instead of
  `CatalogLayout`, duplicated between formats.
- Tests are coupled to the implementation, or assert less than the behavior.
- A user-visible change is missing from `README.md`; a changed design decision or recipe is
  missing from these guides.
- A name, a message or a comment misleads the reader.

**Nit**: optional, say so.

- Style and wording preferences that the build does not enforce.

## How to write findings

- Start with a verdict in one line: ready, ready after fixes, or needs rework.
- Then the findings, most severe first. For each: the file and line, what is wrong, why it
  matters, and a concrete suggestion.
- Say what you did not verify.
- Do not invent findings to look thorough, and do not pad with praise. "No blocking issue
  found" is a valid review.
- Comment on the code, never on the person.

## When your own change is reviewed

Judge each comment on its merits. If it is right, fix the cause, not only the line that was
pointed at. If you disagree, say why with facts (a test, the documentation, a design decision
from [architecture.md](architecture.md)) and let the human decide.
