# Branches, commits and pull requests

The conventions are defined in [CONTRIBUTING.md](../../CONTRIBUTING.md). This guide adds what
an agent needs to apply them without guessing.

**Git is opt-in.** Create a branch, commit, push or open a pull request only when the human
asks for it. When they do, do exactly that step: "commit" does not mean "push".

## Branches

- `main` holds releases only. `develop` is the integration branch. Both are protected: nothing
  is pushed to them directly, and only the maintainer merges `develop` into `main`.
- Start every branch from an up-to-date `develop`.

Name: `<github-user>/<type>/<issue>`, checked by the CI with
`^[A-Za-z0-9-]+/(feature|fix|tech|docs|test)/[a-z0-9._-]+$`.

- `<github-user>` is the GitHub login of the human you work for. Take it from an existing
  branch or ask. Do not derive it from `git config user.name`, which is a display name.
- `<type>` describes the main purpose of the branch:

  | The branch mostly | Type |
  |---|---|
  | adds something users can use | `feature` |
  | repairs a wrong behavior | `fix` |
  | changes build, CI, dependencies or structure without changing behavior | `tech` |
  | changes documentation only | `docs` |
  | changes tests only | `test` |

- `<issue>` is the issue number, or two to four lowercase words joined by dashes when there is
  no issue: `octocat/fix/42`, `octocat/feature/unused-steps`.

If a branch would need two types, it probably holds two pull requests.

## Commits

Format: [Conventional Commits](https://www.conventionalcommits.org), `type(scope): summary`.

- **Type**: `feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `build` or `chore`. Note that the
  commit type is `feat`, while the branch type is `feature`.
- **Scope** (optional): the module or area touched, for example `core`, `render`, `lint`, `plugin`.
- **Summary**: imperative, starting in lower case, no final period, about 70 characters at most.
- **Body** (when the summary is not enough): why the change is needed and what it implies.
  The diff already shows what changed.

Rules:

- One logical change per commit, and every commit builds. Keep a restructuring and a behavior
  change in separate commits.
- Review your own diff before committing (`git status`, `git diff --staged`): no stray file, no
  debug output, nothing under `target/`, no IDE file, no secret.
- Run `./mvnw verify` before the last commit of a branch.
- The human is the author. Do not add `Co-Authored-By` trailers for AI tools, session links or
  "generated with" lines, and do not change the Git identity.

## Pull requests

- Target `develop`. The only pull request ever opened against `main` is the release, by the maintainer.
- Title in the commit format. Fill in the template of `.github/pull_request_template.md`:
  what changes and why, the linked issue (`Closes #42`), and the checklist, ticked honestly.
- Add what a reviewer cannot see in the diff: how you verified the change, the assumptions you
  made, what you left out.
- One topic per pull request. Small ones get reviewed; large ones get postponed.
- Required checks: **CI result** and, once the repository is public, **CodeQL**. If a check
  fails, read its log and fix the cause. Never edit a workflow to make a check pass.

## Keeping a branch up to date

Merge `develop` into the branch. Do not rebase or force-push a branch that is already pushed,
unless the human explicitly asks: a rewritten history breaks their local copy and the review.

## Never

- Push to `main` or `develop`, or merge a pull request yourself.
- Force-push, delete a remote branch or rewrite pushed history on your own initiative.
- Skip hooks or checks (`--no-verify`, `-DskipTests` to get a commit through).
- Commit on the human's behalf after they said "do not commit yet".
