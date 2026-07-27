# Slimefun Legacy — Second Maintenance Release

Status: implementation complete; dependency-resolved CI and staging-server validation required before deployment

This release finishes the Part 2 maintenance work without removing the legacy addon entry points protected by Slimefun Legacy's compatibility checks.

## Player-facing Cargo connector fix

Cargo and energy connector checks now consistently display:

- connected: `Connected: ✔`
- disconnected: `Connected: ✕`

The hardcoded `connectstate:` / `connectedstate:` wording is removed. Internal storage keys and network state are unchanged.

## Scheduler abstraction and ownership migration

Slimefun core scheduling now flows through the tracked `SlimefunScheduler` service.

Implemented scheduling modes:

- global immediate, delayed, and repeating tasks
- location-owned immediate, delayed, and repeating tasks
- entity-owned immediate, delayed, and repeating tasks
- asynchronous immediate, delayed, and repeating tasks
- scheduler-neutral `TaskHandle` cancellation
- centralized shutdown cancellation

Standard Paper retains Bukkit's tick-based timing. Folia-capable servers route known location- and entity-bound work through the corresponding region or entity scheduler.

The maintenance pass migrates the core ticker, storage loading, Item Doctor, armor and radiation processing, recipe-choice animation, teleports, research progression, machine animations, reactors, runes, Android work, Cargo/energy actions, holograms, chat callbacks, GitHub checks, profiler work, and command callbacks. Direct Bukkit scheduler usage is isolated to the scheduler implementation plus a deliberate shutdown fallback.

The historical `Slimefun.runSync(...)` signatures still return `BukkitTask`. A `LegacyBukkitTask` adapter preserves that return shape while routing execution through the tracked scheduler. New `runSyncAt(...)` and `runSyncFor(...)` overloads provide location and entity ownership to internal callers.

## Modern BlockTicker and energy overloads

### BlockTicker

A storage-neutral overload is available:

```java
tick(Block block, SlimefunItem item, ASlimefunDataContainer data)
```

It dispatches to the existing block or universal data overload. Existing `SlimefunBlockData`, `SlimefunUniversalData`, and deprecated `Config` override paths remain available for addon compatibility.

### Energy

Long-capacity energy operations now support already-resolved `ASlimefunDataContainer` instances for charge reads and set/add/remove mutations.

The long setter no longer uses legacy integer capacity or charge accessors. Capacity checks, clamping, overflow-safe addition, and texture updates use long values throughout, including capacities above `Integer.MAX_VALUE`.

## API and internal annotations

The release adds class-retained annotations:

- `@SlimefunAPI` for supported addon-facing contracts
- `@SlimefunInternal` for implementation details

The annotation inventory is complete for every public top-level type in the same package prefixes monitored by the binary API compatibility workflow:

- `io.github.thebusybiscuit.slimefun4.api`
- `io.github.thebusybiscuit.slimefun4.core.attributes`
- `io.github.thebusybiscuit.slimefun4.core.services.scheduling`
- `me.mrCookieSlime.Slimefun.Objects.handlers`
- `me.mrCookieSlime.Slimefun.api`

`scripts/check_api_annotations.py` prevents future public types in those boundaries from being left unclassified.

## Protection compatibility tests

Cargo nodes and legacy inventory blocks use one fail-closed protection policy.

The server-independent test matrix verifies:

- explicit bypass skips optional provider checks
- Slimefun's local denial remains authoritative
- provider allow and deny decisions are preserved
- provider runtime failures deny access
- missing or incompatible provider linkage denies access

This prevents broken optional protection integrations from silently granting access.

## Paper cleanup

The maintenance pass includes:

- Paper `AsyncChatEvent` with Adventure plain-text extraction
- Adventure action-bar messages instead of legacy Bungee action-bar dispatch
- thread-safe chat catcher, radiation grace-period, Cargo/altar/hook, elevator, and profiler state where scheduling can cross ownership boundaries
- entity- and location-owned callbacks for Bukkit world and inventory access
- nonblocking callable helpers backed by `CompletableFuture`
- `getTargetBlockExact(...)` for target lookup
- removal of legacy `scheduleSync...` and `BukkitRunnable` usage from core
- corrected wall-clock/nanosecond comparison in slow SQL detection
- corrected GitHub polling period units
- isolated suppression for the unit-test-only legacy `JavaPluginLoader` constructor

## Compatibility policy

This update is additive across the protected addon API surface. Legacy ticker overloads, storage bridges, integer energy methods, and `Slimefun.runSync(...)` descriptors remain present. New scheduler helpers and long-energy/container overloads do not replace existing public descriptors.

## Required release validation

Run the authoritative build in a dependency-enabled environment:

```bash
python3 scripts/verify_english.py .
python3 scripts/verify_chunk_load_threading.py .
python3 scripts/check_api_annotations.py
python3 scripts/verify_part2.py .
./gradlew spotlessCheck clean build --no-daemon
```

Then test the resulting JAR on a staging Paper server with the Albion addon set before production deployment.
