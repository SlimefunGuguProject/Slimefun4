#!/usr/bin/env python3
"""Verify Paper-safe Slimefun chunk loading in the Stability Release."""

from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
controller = (
    root
    / "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/controller/BlockDataController.java"
).read_text(encoding="utf-8")
doctor = (
    root
    / "src/main/java/io/github/thebusybiscuit/slimefun4/core/services/stability/ItemDoctorService.java"
).read_text(encoding="utf-8")

failures: list[str] = []

if "CompletableFuture.runAsync(() -> loadChunk" in controller:
    failures.append("BlockDataController still loads chunks on a CompletableFuture executor")
if "BukkitTask task = Slimefun.runSync" not in controller:
    failures.append("BlockDataController async API does not marshal chunk loading to the server thread")
if "controller.getChunkDataAsync(chunk)" in doctor:
    failures.append("ItemDoctorService still requests async storage loading from ChunkLoadEvent")
if "onSlimefunChunkDataLoad(SlimefunChunkDataLoadEvent event)" not in doctor:
    failures.append("ItemDoctorService is not listening for completed Slimefun chunk data loads")

if failures:
    for failure in failures:
        print(f"ERROR: {failure}", file=sys.stderr)
    raise SystemExit(1)

print("Chunk-load threading verification passed.")
