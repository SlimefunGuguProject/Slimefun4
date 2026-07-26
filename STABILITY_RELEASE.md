# Slimefun Legacy — Stability Release 1

Build version: `Legacy-Stability-1-Hotfix-1`

This release is the first Albion Slimefun Legacy stability release. It combines compatibility safeguards, storage recovery tooling, machine fault isolation, backpack race protection, and Cargo performance work without introducing the separate Part 2 API modernization changes.

## Hotfix 1: Paper 26.2 chunk-load thread safety

This package includes the runtime fix for `SlimefunChunkDataLoadEvent may only be triggered synchronously`. The Item Doctor now waits for `SlimefunChunkDataLoadEvent` instead of requesting an asynchronous chunk load from `ChunkLoadEvent`. The shared `getChunkDataAsync` API also schedules unloaded chunk initialization on the primary server thread, preventing GEO systems and addons from triggering the same Paper exception. Slimefun machine menus are repaired after their database data is ready, using bounded two-tick retries without blocking a server tick.

## Main additions

### Slimefun Storage and Item Doctor

The operator command is available as either `/slimefun doctor` or `/sf doctor`.

| Command | Purpose |
| --- | --- |
| `/sf doctor status` | Shows clean-shutdown state, pending database writes, paused machine circuits, automatic repairs, and the current/last doctor run. |
| `/sf doctor hand` | Repairs the Slimefun item held by the executing player. |
| `/sf doctor inventory [player]` | Repairs an online player's inventory and ender chest. |
| `/sf doctor scan` | Runs a batched server-wide dry run. No items are changed. |
| `/sf doctor repair confirm` | Runs the batched server-wide repair. |

Permission: `slimefun.command.doctor` (operator by default).

The doctor identifies an item through its persistent Slimefun ID. It does not infer identity from translated text. It changes only visible display names and lore, then restores recognized dynamic presentation from the original item.

Preserved data includes:

- Slimefun item ID and persistent data
- enchantments, attributes, custom model data, stack amount, and material
- energy charge and remaining-use counts
- monster-spawner type
- Soulbound state
- Knowledge Tome owner identity
- current and legacy backpack identity and ownership
- items stored inside bundles and shulker boxes
- addon lore numbers and UUIDs when they map safely to the registered English template

Unknown Slimefun IDs, templates that remain translated, malformed state, and ambiguous dynamic lore are reported and skipped. The doctor does not guess.

### Repair coverage

The server-wide run covers:

- online player inventories and ender chests
- loaded chests, barrels, entities, machines, and dropped items
- loaded Slimefun block and universal inventories
- every Slimefun backpack stored in the configured database
- nested bundles and shulker boxes, up to four container levels

Offline player inventories are repaired when the player next joins. Unloaded world storage is repaired when its chunk or inventory is next loaded. These automatic paths are configurable under `stability.item-doctor` in `config.yml`.

## Stability work included

- Backpack duplicate-open protection and disconnect/failure cleanup
- Race-safe maintenance loading for database backpack scans
- Cargo network topology and allocation optimization for issue #1223
- Clean-shutdown marker and pending-write visibility
- Per-machine ticker circuit breaker with cooldown and administrator retry commands
- Viewer, ticker, and chunk lifecycle regression coverage
- Addon compatibility CI against the exact built Slimefun JAR
- Public API binary compatibility reporting

## Recommended rollout

1. Stop the server normally.
2. Back up the full server, all worlds, player data, and the complete `plugins/Slimefun` directory/database.
3. Install the release on a staging copy first.
4. Start the server and run `/sf doctor status`.
5. Run `/sf doctor scan` and review unknown IDs, unresolved templates, and failures.
6. Test representative machines, backpacks, Cargo networks, recipes, and addon items.
7. Run `/sf doctor repair confirm` only after the dry run looks correct.
8. Keep the server running until `/sf doctor status` reports `0` pending database writes.
9. Stop the server normally and start it again.
10. Spot-check repaired items in player storage, chests, machines, backpacks, shulkers, and bundles.

Do not use `/reload` during a repair. Do not force-kill the server while database writes are pending.

## Configuration

```yaml
stability:
  machine-circuit-breaker-cooldown-seconds: 300
  item-doctor:
    enabled: true
    repair-player-on-join: true
    repair-opened-inventories: true
    repair-chunks-on-load: true
    repair-picked-up-items: true
    inventories-per-tick: 12
```

Missing settings receive these defaults during configuration loading, so upgrades from an older `config.yml` still enable the intended behavior.

## Building

The manual GitHub workflow **Build Stability Release** performs the authoritative release build with Java 25 while targeting Java 21 bytecode. It runs English verification, Spotless, tests, the shaded build, source packaging, and SHA-256 generation.

Local command:

```bash
chmod +x gradlew
./gradlew spotlessApply --no-daemon
./gradlew spotlessCheck clean build -PprojectVersion=Legacy-Stability-1-Hotfix-1 --no-daemon
```

Expected primary artifact:

```text
build/libs/Slimefun-Legacy-Stability-1-Hotfix-1.jar
```
