# Second maintenance release validation

## Passed in this assembly

- Exact connector values validated as `Connected: ✔` and `Connected: ✕`.
- No `connectstate` or `connectedstate` text remains in main sources.
- No direct Bukkit scheduler calls remain in core outside `PaperScheduler` and the intentional plugin-shutdown fallback.
- Scheduler abstraction and the legacy `BukkitTask` adapter compiled against Paper/Bukkit API-shaped Java 21 stubs.
- Scheduler runtime smoke passed for one-shot execution, repeating cancellation, legacy adapter behavior, `cancelAll()`, and rejection after shutdown.
- Folia-style entity retirement smoke passed for immediate and delayed cleanup callbacks.
- Storage-neutral `BlockTicker` dispatch smoke passed for legacy block, universal storage, and rejected mismatched storage.
- Long-capacity energy runtime smoke passed above `Integer.MAX_VALUE`, including clamping and overflow-safe addition/removal.
- Pure-Java annotation and fail-closed protection smoke checks passed.
- API annotation inventory passed for all compatibility-protected package prefixes.
- Chunk-load ownership verification passed.
- Item Doctor owner-dispatch and pending-work completion invariants passed.
- Delayed research unlock state includes disconnect/retirement cleanup.
- Second-maintenance static release verification passed.
- English runtime-text verification passed.
- A full-source `javac` parse pass found no Java syntax errors; unresolved external dependencies were expected without the Gradle classpath.
- Git patch whitespace validation passed.

## Dependency-resolved build status

The Gradle 9.4.1 wrapper distribution and dependency cache are not available in this assembly environment. The wrapper failed while resolving `services.gradle.org` with `UnknownHostException`, so Spotless, JUnit, ShadowJar, and the complete Gradle build could not be executed here.

No unverified JAR is included. Run:

```bash
./gradlew spotlessCheck clean build --no-daemon
```

The included `second-maintenance-release.yml` workflow performs the static checks, Java 25 build with Java 21 bytecode target, tests, source packaging, and checksum generation.
