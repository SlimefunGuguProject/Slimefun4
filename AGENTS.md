# AGENTS.md

## Project facts
- This is a Gradle Kotlin DSL Java plugin project for Bukkit/Paper; there is no Maven `pom.xml` despite `.github/workflows/copilot-setup-steps.yml` running `mvn`.
- Runtime entrypoint is `src/main/resources/plugin.yml` -> `io.github.thebusybiscuit.slimefun4.implementation.Slimefun`.
- The root Gradle project is named `Slimefun`; `build` produces the shaded plugin jar via `shadowJar`, and the plain `jar` task is disabled.
- Main namespaces are mixed intentionally: upstream/core code under `io.github.thebusybiscuit.slimefun4.*`, fork-specific code under `city.norain.slimefun4.*`, and storage/chat migration glue under `com.xzavier0722.*`.

## Commands
- Windows: `./gradlew.bat build`; Unix/CI: `./gradlew build --no-daemon`.
- Format check: `./gradlew spotlessCheck --no-daemon`.
- Apply formatting only when intended: `./gradlew spotlessApply --no-daemon`.
- Run tests: `./gradlew test --no-daemon`.
- Run one JUnit test class: `./gradlew test --tests io.github.thebusybiscuit.slimefun4.utils.TestMinecraftVersion --no-daemon`.
- CI PR order is `spotlessCheck` then `build -PprojectVersion="Preview Build #<pr>-<shortsha>"`, followed by e2e server startup tests.

## Build and release quirks
- Java 21 is authoritative: Gradle toolchain and `options.release` are both set to 21; CI defaults to Java 21.
- Version defaults to `DEV-SNAPSHOT`; pass `-PprojectVersion=...` when the jar/plugin version must be specific.
- `processResources` expands `${version}` in `plugin.yml` and depends on generated `git.properties`; `sourcesJar` also depends on `generateGitProperties`.
- Shading relocates Dough, PaperLib, Unirest, Commons Lang, and GuizhanLib into `io.github.thebusybiscuit.slimefun4.libraries.*`; do not change relocations casually.
- Many server/integration dependencies are `compileOnly` because they are soft dependencies loaded by the server (`PlaceholderAPI`, `WorldEdit`, `ClearLag`, `mcMMO`, `ItemsAdder`, `Vault`, `Orebfuscator`).
- Publishing needs `MAVEN_ACCOUNT` and `MAVEN_PASSWORD`; snapshots/releases go to `https://maven.norain.city/` based on whether the version contains `SNAPSHOT`.

## Style and tests
- Java formatting is Spotless with Palantir Java Format `2.90.0`, plus unused-import removal and annotation formatting.
- `.editorconfig` enforces LF, UTF-8, Java 4-space indents, 120-column max, and single-class imports.
- Existing unit tests are sparse; e2e coverage in CI downloads Paper versions `1.17.1`, `1.18.2`, `1.19.4`, `1.20.6`, `1.21.4`, `1.21.8`, and `latest`, then boots the built plugin with an external `Slimefun-e2e-tester` jar.

## Repo-specific cautions
- README support/version text may lag executable config; trust `build.gradle.kts`, `gradle/libs.versions.toml`, and CI over prose when they conflict.
- This fork contains Chinese logs/comments/docs; do not translate user-facing text unless the task asks for localization changes.
- `src/main/resources/plugin.yml` still declares `api-version: '1.16'` while compiling against Paper API `1.21.11-R0.1-SNAPSHOT`; do not “modernize” it without verifying compatibility intent.
