# Releasing

For the maintainer. A release is a merge of `develop` into `main`: the
[release workflow](../.github/workflows/release.yml) then builds, signs and uploads the version
written in the POM to Maven Central, and creates the tag and the GitHub release.

Nothing becomes public on Maven Central until you publish the upload by hand, and that last
step cannot be undone: a published version can never be changed or removed.

## One-time setup

1. **Namespace.** `com.exodius-software` is verified in the
   [Central Portal](https://central.sonatype.com/publishing/namespaces).
2. **Signing key.** A GPG key whose public part is on `keyserver.ubuntu.com`. Maven Central
   only verifies signatures made with the primary key, so the key must not have a signing subkey.
3. **Environment.** A GitHub environment named `maven-central`, restricted to the `main`
   branch, with these secrets:

   | Secret | Value |
   |---|---|
   | `CENTRAL_USERNAME` | user name of a [Portal user token](https://central.sonatype.com/usertoken) |
   | `CENTRAL_PASSWORD` | password of that token |
   | `MAVEN_GPG_KEY` | the private key, as printed by `gpg --armor --export-secret-keys <KEYID>` |
   | `MAVEN_GPG_PASSPHRASE` | passphrase of the key |

When the key or the token expires, replace the secret; nothing changes in the repository.

## Release a version

Versions follow [Semantic Versioning](https://semver.org); `CHANGELOG.md` says what they cover.
In the commands below, replace `1.2.3` with the version you release.

1. **Prepare it on `develop`**, in a branch named `<you>/tech/release-1.2.3`:

   ```shell
   ./mvnw versions:set -DnewVersion=1.2.3 -DgenerateBackupPoms=false
   ```

   Then, by hand:
   - `CHANGELOG.md`: rename "Unreleased" to `[1.2.3] - YYYY-MM-DD` and add an empty
     "Unreleased" section above it;
   - `README.md`, `docs/user-guide.md` and `examples/bookshop/pom.xml`: replace the previous
     version with `1.2.3`. For the first release, also remove the paragraphs that tell users
     to install StepDoc locally.

   Open the pull request, wait for the CI, merge.

2. **Check the bundle** (optional, needs a key in `MAVEN_GPG_KEY` and `MAVEN_GPG_PASSPHRASE`):

   ```shell
   ./mvnw -Prelease deploy -Drelease.skipUpload=true
   ```

   This builds and signs `target/central-publishing/central-bundle.zip` without sending it.

3. **Release**: open a pull request from `develop` to `main` and merge it. The release workflow
   starts. It refuses a version that is not a plain `x.y.z` or that is already tagged.

4. **Publish**: in the Portal, open
   [Deployments](https://central.sonatype.com/publishing/deployments). The deployment
   "StepDoc 1.2.3" must be "Validated". Look at its content, then press **Publish**.
   The artifacts then appear on Maven Central; the search pages can lag behind.

5. **Go back to development**, in a branch named `<you>/tech/next-version`:

   ```shell
   ./mvnw versions:set -DnewVersion=1.3.0-SNAPSHOT -DgenerateBackupPoms=false
   ```

   Put the same version in `examples/bookshop/pom.xml`: the example runs against the build of
   the repository. `README.md` and the user guide keep the released version, the one users install.

## If something goes wrong

- **The workflow fails before the upload** (build, tests, signature): nothing was sent. Fix it
  on `develop`, merge into `main` again.
- **Central refuses the deployment** (status "Failed"): the workflow fails at the upload and
  creates no tag. The Portal says why. Press **Drop**, fix on `develop`, merge into `main` again.
- **You decide not to publish a validated deployment**: press **Drop**, then delete the GitHub
  release and the tag `v1.2.3`. The version number can be reused: it was never published.
- **You notice a problem after publishing**: the version stays. Release a fixed `1.2.4`.
