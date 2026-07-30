#!/usr/bin/env python3
"""Static invariants for the first Slimefun Legacy Folia hardening phase."""

from __future__ import annotations

import sys
from pathlib import Path


def require(condition: bool, message: str, failures: list[str]) -> None:
    if not condition:
        failures.append(message)


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    failures: list[str] = []

    def text(relative: str) -> str:
        return (root / relative).read_text(encoding="utf-8")

    plugin_yml = text("src/main/resources/plugin.yml")
    scheduler = text(
        "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/scheduling/PaperScheduler.java"
    )
    ticker = text("src/main/java/io/github/thebusybiscuit/slimefun4/implementation/tasks/TickerTask.java")
    block_ticker = text("src/main/java/me/mrCookieSlime/Slimefun/Objects/handlers/BlockTicker.java")
    network = text("src/main/java/io/github/thebusybiscuit/slimefun4/api/network/Network.java")
    event_threading = text("src/main/java/io/github/thebusybiscuit/slimefun4/api/events/EventThreading.java")

    require("folia-supported: true" in plugin_yml, "plugin.yml must opt into Folia loading", failures)
    require("Bukkit.getRegionScheduler()" in scheduler, "region scheduler path is missing", failures)
    require("entity.getScheduler()" in scheduler, "entity scheduler path is missing", failures)
    require("Bukkit.getGlobalRegionScheduler()" in scheduler, "global-region scheduler path is missing", failures)
    require("runFoliaCycle" in ticker and "runAt(anchor" in ticker, "machine ticks are not region-dispatched", failures)
    require("AtomicBoolean running" in ticker, "machine cycles are not protected from overlap", failures)
    require("foliaTickerLocks" in ticker, "shared addon BlockTicker instances are not serialized", failures)
    require("public synchronized void update()" in block_ticker, "BlockTicker update transition is not synchronized", failures)
    require(
        "public synchronized void startNewTick()" in block_ticker,
        "BlockTicker cycle reset is not synchronized",
        failures,
    )
    require("ConcurrentLinkedQueue" in network, "network discovery queue is not concurrent", failures)
    require("isLocationAccessible" in network, "Folia network ownership boundary is missing", failures)
    require(
        "Bukkit.isOwnedByCurrentRegion(location)" in event_threading,
        "location-owned event threading detection is missing",
        failures,
    )
    require(
        "Bukkit.isOwnedByCurrentRegion(entity)" in event_threading,
        "entity-owned event threading detection is missing",
        failures,
    )

    direct_scheduler_uses: list[str] = []
    for path in (root / "src/main/java").rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        if "Bukkit.getScheduler()" not in source:
            continue
        relative = path.relative_to(root).as_posix()
        if relative.endswith("implementation/scheduling/PaperScheduler.java"):
            continue
        if relative.endswith("implementation/Slimefun.java") and "if (!schedulerService.isFolia())" in source:
            continue
        direct_scheduler_uses.append(relative)

    require(
        not direct_scheduler_uses,
        "unguarded direct Bukkit scheduler usage: " + ", ".join(direct_scheduler_uses),
        failures,
    )

    if failures:
        print("Folia Phase 1 verification failed:")
        for failure in failures:
            print(f" - {failure}")
        return 1

    print("Folia Phase 1 static verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
