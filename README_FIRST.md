# Slimefun Legacy Stability Release 1 — Hotfix 1

This source tree contains the complete Stability Release 1 plus the Paper 26.2
chunk-load thread-safety hotfix.

## Fixed runtime error

The hotfix resolves:

```text
SlimefunChunkDataLoadEvent may only be triggered synchronously
```

The Item Doctor no longer calls the asynchronous chunk loader from
`ChunkLoadEvent`. It repairs Slimefun machine menus after the storage controller
fires its synchronous load event. The shared `getChunkDataAsync` implementation
also marshals unloaded chunk initialization through the primary server thread,
preventing the same exception in GEO systems and addons.

## GitHub use

This archive is rooted at the repository root. Upload or extract the files into
your Slimefun Legacy repository, commit them, and run the included GitHub Actions
workflow **Build Stability Release**. Its default version is:

```text
Legacy-Stability-1-Hotfix-1
```

## Local build

```bash
chmod +x gradlew
./gradlew spotlessApply --no-daemon
./gradlew spotlessCheck clean build \
  -PprojectVersion=Legacy-Stability-1-Hotfix-1 --no-daemon
```

Expected JAR:

```text
build/libs/Slimefun-Legacy-Stability-1-Hotfix-1.jar
```

No locally compiled JAR is included because this assembly environment could not
resolve `services.gradle.org`. Source parsing, English verification, YAML
validation, thread-safety verification, and the standalone stability harnesses
passed. Review `HOTFIX_VALIDATION.md` for the exact checks.

Back up the server, worlds, and Slimefun databases before replacing the plugin.
Do not use `/reload`; perform a full clean restart.
