# Slimefun Legacy — Second Maintenance Release, Part 2 Foundation

Status: source foundation for the second maintenance release

This package begins Part 2 without removing legacy addon entry points. It fixes the visible Cargo/energy connector state text and introduces the scheduler, ticker, energy, API-boundary, protection, and Paper cleanup foundations needed for the remaining maintenance work.

## User-visible fix

Cargo and energy connector checks now use one localized result:

- connected: `Connected: ✔`
- disconnected: `Connected: ✘`

The hardcoded `connectstate:` text has been removed. Internal storage and network state keys were not renamed.

## Scheduler abstraction

Added a tracked `SlimefunScheduler` and `TaskHandle` API with:

- global, location, entity, and asynchronous scheduling
- delayed and fixed-rate variants
- location ownership checks
- centralized cancellation during plugin shutdown
- standard Paper tick scheduling through Bukkit
- Folia-aware global, region, entity, and asynchronous backends

Initial migrations cover the machine ticker, synchronized machine execution, player tasks, autosaving, hologram work, chat callbacks, and integration startup. Existing `Slimefun.runSync(...)` methods remain for addon and internal compatibility while migration continues.

This is a foundation, not a claim that every legacy task is Folia-safe. Storage loading, armor tasks, profiler tasks, and other remaining direct Bukkit scheduler users still need later migration and testing.

## Modern BlockTicker and energy overloads

### BlockTicker

Added a storage-neutral overload:

```java
tick(Block block, SlimefunItem item, ASlimefunDataContainer data)
```

It dispatches to the existing block or universal overload. Existing addon overrides and the deprecated `Config` bridge are retained.

### Energy

Added long-capacity overloads that accept a resolved `ASlimefunDataContainer` for set, add, and remove operations. Location-only long methods now delegate through the same path.

The prior long-charge setter incorrectly used the legacy integer capacity and charge accessors. It now uses `getCapacityLong()` and `getChargeLong(...)`, preserving values above `Integer.MAX_VALUE`.

## API and internal annotations

Added class-retained annotations:

- `@SlimefunAPI` for supported addon-facing contracts
- `@SlimefunInternal` for implementation details

The first annotation pass covers core addon/item handlers, `SlimefunItem`, `BlockTicker`, energy components, scheduler contracts, storage compatibility wrappers, ticker internals, and protection internals. This is intentionally incremental.

## Protection compatibility tests

Added a server-independent, fail-closed protection policy used by Cargo and legacy inventory menus.

Coverage verifies:

- explicit bypass permissions still bypass provider checks
- local Slimefun permission denial is preserved
- provider allow/deny results are preserved
- broken optional protection integrations deny access rather than silently allowing it

## Paper deprecation cleanup

The first cleanup pass includes:

- Paper `AsyncChatEvent` and Adventure plain-text extraction
- thread-safe chat-catcher storage
- entity-scheduled chat callbacks and player tasks
- nonblocking callable helpers using `CompletableFuture`
- removal of old `scheduleSyncDelayedTask` and `scheduleSyncRepeatingTask` calls
- synchronous recipe-choice inventory mutation instead of async Bukkit inventory access
- `getTargetBlockExact(...)` for the Storm Staff
- an actual `@Deprecated` marker on the legacy `InventoryBlock`
- suppression isolated to the unit-test-only legacy `JavaPluginLoader` constructor

## Compatibility boundaries

This foundation deliberately does not remove or change the descriptors of existing public methods. Legacy ticker, storage, and Bukkit task bridges remain available. The new overloads and annotations are additive.

Before promoting this source to a production release, the remaining Part 2 work should include:

1. migrate the remaining core scheduler call sites by ownership type
2. add dependency-resolved Paper integration tests
3. run addon binary compatibility CI against the resulting shaded JAR
4. complete the annotation inventory for the public API surface
5. run the authoritative Java 25 / Java 21-bytecode build and staging-server tests

## Validation completed in this assembly

- targeted Java compilation against Paper/Bukkit API-shaped stubs for the scheduler, BlockTicker, energy API, chat migration, and hologram service
- scheduler runtime smoke test for execution, cancellation, shutdown rejection, and task tracking
- long-energy runtime smoke test above the integer limit
- pure-Java smoke tests for annotations, protection behavior, and tick conversion
- English YAML parsing and exact Cargo message checks
- scan confirming no remaining `connectstate` or `connectedstate` text
- scan confirming no remaining `scheduleSyncDelayedTask` or `scheduleSyncRepeatingTask` calls
- changed-file whitespace and patch validation

The local Gradle build could not run because the Gradle 9.4.1 wrapper distribution was not available in the container and the container could not resolve `services.gradle.org`. No locally unverified JAR is included. Run the repository build workflow or a dependency-enabled local build before deployment.
