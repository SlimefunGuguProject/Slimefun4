# Slimefun Legacy — Fourth Maintenance Release

## Folia Event Safety & Paper API Cleanup

This release keeps standard Paper as the primary production platform while completing another focused pass toward Folia compatibility.

### Event-state safety

- Soulbound recovery state now uses concurrent storage.
- Soulbound items are snapshotted and are not re-added when `keepInventory` is active.
- Elytra impact grace state now uses a concurrent set, entity-owned delayed cleanup, and disconnect cleanup.
- Slimefun bow projectile state now uses a concurrent map and entity-owned retirement-safe cleanup.

### Paper API cleanup

- Vanilla Auto-Crafters use `GameRules.LIMITED_CRAFTING`.
- Auto Brewer and potion comparison code use the modern `PotionMeta` base-potion-type API.
- Wind Staff and Storm Staff use the current `FoodLevelChangeEvent` constructor.
- `/sf versions` now recommends Java 21 or newer and reports whether Slimefun is using Paper or Folia scheduler semantics.

### Profiler reliability

- Empty profiler windows now return zero instead of dividing by zero.
- Millisecond and nanosecond averages use independent sample counters and can no longer reset one another.

### Folia status

`folia-supported: true` is now declared because core scheduling is routed through the scheduler abstraction and the event-state paths covered by this release are ownership-aware. Folia deployment must still be tested on a copied server, and every installed addon must independently support Folia.
