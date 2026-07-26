# Albion merge notes

## Base selection

The uploaded Gugu source was selected as the new base rather than merging its files
into the older United tree. The projects have diverged in their build system and code
layout: the Gugu base uses Gradle, Java 21, and newer Paper APIs, while the previous
English source used an older Maven-based structure.

Using Gugu as the base avoids discarding newer storage, security, API, and maintenance
changes. English and Albion-specific behavior is then carried as a reviewable patch.

## English conversion

The imported source contained Chinese text in item definitions and many runtime paths.
The merge performs the following:

- Ports matching English item names and lore from the previous United source.
- Manually translates remaining runtime strings that do not have a safe donor match.
- Forces the configured server language whenever per-player translations are disabled.
- Sets the shipped defaults to English-only.
- Keeps optional language packs for compatibility while ensuring they cannot override
  English in the default setup.
- Adds a verifier that scans Java string literals and installed default resource files.

Chinese developer comments may remain because comments are not compiled or displayed
to players. The verifier intentionally distinguishes comments from runtime strings.

## Albion compatibility retained

`RadiationDamageEvent` and its cancellable event hook were carried forward from the
previous Albion/United source. Older helper classes that were superseded by the newer
Gugu architecture were not copied, because doing so would duplicate or conflict with
newer implementations.

## Update design

The source patch lives at `patches/albion-english.patch`. The scheduled workflow starts
from a clean Gugu archive on every run, reapplies that patch, verifies English, and
builds before opening a pull request.

A patch conflict is treated as a failed workflow, not silently resolved. This prevents
an upstream refactor from accidentally deleting English text or Albion compatibility.

## Stability Release 1 retention

The maintained patch includes the Storage and Item Doctor, backpack maintenance-cache safety,
limited-use and spawner presentation recovery APIs, configuration defaults, permissions, tests,
and command/service registration. The upstream synchronization workflow therefore cannot replace
`src/` with a fresh Gugu tree and silently remove `/slimefun doctor`.

The patch was reverse-applied to reconstruct the clean supplied upstream base, regenerated from
the final source, reapplied with `scripts/sync_upstream.sh`, and compared byte-for-byte across
`src/`, `build.gradle.kts`, and `gradle/libs.versions.toml`.

## Verification completed for this source package

The supplied upstream contained 1,692 Java string literals with CJK text, including
1,123 in `SlimefunItems.java`. The merged source contains zero CJK Java string
literals. The maintained patch now changes 135 source, resource, test, and build files. It includes the Albion
radiation event plus the complete Stability Release 1 implementation, so scheduled upstream syncs retain these fixes.

- Player-facing Java string scan: passed.
- Installed default resource scan: passed.
- English configuration guard: passed.
- Java parser syntax scan: 801 source and test files parsed with no syntax errors.
- Patch round-trip against the supplied Gugu source: passed; the regenerated tree is byte-for-byte identical under `src/`.

A dependency-resolved Gradle build could not be completed in the offline assembly
environment because the Gradle distribution and Maven dependencies were unavailable.
The included GitHub build workflow performs that check on an internet-connected runner.
