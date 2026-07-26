# Stability Release 1 Hotfix 1 validation

## Reported failure

Paper 26.2 rejected `SlimefunChunkDataLoadEvent` because Item Doctor called
`BlockDataController.getChunkDataAsync()` during a chunk-load hook. The async
implementation invoked `loadChunk()` on a database executor, and `loadChunk()`
fires a synchronous Bukkit event.

## Code corrections

1. `ItemDoctorService`
   - Removed the asynchronous storage request from `ChunkLoadEvent`.
   - Added a listener for `SlimefunChunkDataLoadEvent`.
   - Added bounded two-tick retries while database-backed block inventories finish loading.
   - Keeps physical chunk inventories and dropped-item repair on the primary server thread.

2. `BlockDataController`
   - `getChunkDataAsync()` now returns a future but schedules unloaded chunk initialization through `Slimefun.runSync()`.
   - The callback overload delegates to the corrected future implementation.
   - Already-loaded chunk data still returns immediately.

3. Release safeguards
   - Added `scripts/verify_chunk_load_threading.py`.
   - Added the verifier to the release workflow.
   - Updated release notes, changelog, and default workflow version.

## Validation completed

- Chunk-load threading verification: PASS
- English runtime-text verification: PASS
- Java parser, production sources: 796 files PASS
- Java parser, test sources: 5 files PASS
- `config.yml`: PASS
- `plugin.yml`: PASS
- `.github/workflows/stability-release.yml`: PASS
- Item Doctor helper harness: PASS
- Backpack cache ownership/concurrency harness: PASS
- Changed-file checksum verification: PASS

## Build limitation

A full Gradle build was attempted, but the assembly environment could not resolve
`services.gradle.org`, so no unverified JAR is included. The GitHub Actions
workflow remains the authoritative compilation, Spotless, JUnit, shading, and
artifact-generation gate.

## Server verification checklist

1. Back up the complete server and Slimefun storage.
2. Build `Legacy-Stability-1-Hotfix-1` with the included workflow.
3. Install the JAR and perform a clean restart.
4. Load several chunks containing Slimefun machines.
5. Confirm the synchronous-event warning no longer appears.
6. Run `/sf doctor scan`.
7. Test a Chinese-labeled item in a player inventory, chest, Slimefun machine, and backpack.
8. Run `/sf doctor repair confirm` only after reviewing the scan results.
