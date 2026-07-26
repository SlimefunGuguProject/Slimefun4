# Stability Release validation

## Required release gates

The release is accepted only when all of the following pass:

- English runtime-text verification
- Spotless formatting verification
- Java compilation targeting Java 21
- JUnit 5 tests
- shaded JAR creation
- addon compatibility CI
- public API compatibility comparison when a prior release JAR is available
- release SHA-256 generation

The manual `.github/workflows/stability-release.yml` workflow performs these gates and packages the JAR, sources JAR, source ZIP, documentation, and checksums.

## Source-level validation completed during assembly

- Parsed every Java source file with the Java compiler parser.
- Ran the server-independent item-doctor text and report regression harness.
- Ran the backpack maintenance-cache concurrency/ownership regression harness.
- Validated `config.yml`, `plugin.yml`, and storage YAML syntax.
- Ran `scripts/verify_english.py` against the assembled source.
- Checked changed files for trailing whitespace and malformed patch output.
- Reverse-applied, regenerated, reapplied, and byte-compared the maintained Albion patch.

## Local build-environment limitation

The assembly container did not have a Gradle installation or a usable dependency/distribution connection, so it could not produce a trustworthy local shaded JAR. No unverified JAR is included in the source package. The GitHub release workflow is intentionally included as the authoritative dependency-resolved build gate and refuses to upload a release bundle if the expected JAR or sources JAR is absent.
