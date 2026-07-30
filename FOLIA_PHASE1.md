# Slimefun Legacy Folia Support — Phase 1

This phase establishes a conservative Folia execution boundary without changing Slimefun Legacy's established Paper behavior or removing legacy addon APIs.

## Implemented

- Central Folia runtime detection shared by scheduler and utility code.
- Location ownership and entity ownership checks in the scheduler API.
- Region-owned machine ticking on Folia, grouped by machine chunk.
- A non-overlapping machine cycle coordinator that waits for every scheduled region chunk before advancing `BlockTicker` cycle state.
- Per-`BlockTicker` serialization on Folia to protect addons that keep mutable state in a shared ticker instance.
- Thread-safe `BlockTicker.uniqueTick()` cycle transitions.
- Entity-owned backpack callbacks, inventory opening/closing, and player callback executors.
- Location-owned compatibility bridges for storage and block operations.
- Owner-aware custom-event asynchronous flags.
- Concurrent Cargo and energy network collections.
- A safe Folia network boundary: discovered topology may be retained, but Cargo and energy only read or mutate nodes owned by the current regulator region.
- Folia-aware shutdown and startup warnings.
- Static verification plus a concurrency regression test for `BlockTicker` cycle state.

## Paper compatibility

Paper continues to use the historical scheduler path:

- asynchronous tickers remain asynchronous;
- synchronized or viewed-inventory tickers are moved to Paper's primary thread;
- Bukkit scheduler cancellation and player inventory closure remain unchanged on Paper;
- no existing public method descriptor was removed.

## Folia behavior change for addons

On Folia, `BlockTicker#isSynchronized()` cannot mean "may access Bukkit asynchronously." Every block ticker is executed by the region that owns its machine location. CPU-only work may still be delegated to the async scheduler, but all Bukkit entity, inventory, block, chunk, and world work must be marshalled back to the correct owner.

A shared `BlockTicker` instance is serialized to reduce breakage in older addons that store mutable counters or temporary state on the ticker object. Addons should still migrate that state to per-location storage when practical.

## Intentional network safety boundary

Phase 1 does **not** attempt cross-region Cargo or energy transactions. A regulator only operates nodes currently owned by its execution region. Nodes outside that ownership boundary are retained as a deferred topology frontier and can be reconsidered after Folia merges regions, but no direct cross-region inventory or energy access occurs.

This prevents a partial implementation from introducing item duplication, item loss, cross-region thread exceptions, or inconsistent charge updates. True cross-region support requires a reserve/commit/rollback transaction coordinator and belongs in a later phase.

## Remaining work before calling Folia fully supported

1. Audit every core item, listener, menu, entity task, and storage callback under a live Folia thread checker.
2. Implement transactional cross-region Cargo transfers.
3. Implement snapshot/commit cross-region energy distribution.
4. Add region split/merge recovery tests and loaded/unloaded chunk tests.
5. Test every supported addon individually; the core cannot make direct unsafe Bukkit calls inside an addon safe.
6. Run duplication, shutdown, restart, database, profiler, and circuit-breaker stress tests on a staging Folia server.

## Staging checklist

- Use a copy of the production world and database.
- Start with Slimefun Legacy only, then add addons one at a time.
- Place identical machines in distant regions and confirm independent ticking.
- Test block menus while machines tick.
- Test Cargo and energy entirely inside one owned region.
- Confirm networks crossing a region boundary pause remote nodes rather than moving items or power unsafely.
- Watch logs for thread-access exceptions, duplicate machine cycles, failed callbacks, and circuit-breaker trips.
- Restart repeatedly and verify inventories, backpacks, energy charge, and block storage.

Folia remains **experimental** after this phase.
