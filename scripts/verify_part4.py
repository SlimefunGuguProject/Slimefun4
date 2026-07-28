#!/usr/bin/env python3
"""Static invariants for the Part 4 Folia event-safety and Paper API cleanup release."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def read(path: str) -> str:
    target = ROOT / path
    if not target.is_file():
        errors.append(f"Missing required file: {path}")
        return ""
    return target.read_text(encoding="utf-8")


def require(path: str, text: str, description: str) -> None:
    if text not in read(path):
        errors.append(f"{description}: {path}")


def forbid(path: str, text: str, description: str) -> None:
    if text in read(path):
        errors.append(f"{description}: {path}")


require("src/main/resources/plugin.yml", "folia-supported: true", "Folia support marker is missing")

soulbound = "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/listeners/SoulboundListener.java"
require(soulbound, "new ConcurrentHashMap<>()", "Soulbound recovery state is not concurrent")
require(soulbound, "priority = EventPriority.HIGHEST", "Soulbound death handling runs before final keepInventory state")
require(soulbound, "if (e.getKeepInventory())", "Soulbound keepInventory duplication guard is missing")
require(soulbound, "item.clone()", "Soulbound recovery does not snapshot ItemStacks")

elytra = "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/listeners/ElytraImpactListener.java"
require(elytra, "ConcurrentHashMap.newKeySet()", "Elytra glide state is not concurrent")
require(elytra, "priority = EventPriority.MONITOR, ignoreCancelled = true", "Elytra state does not observe the final glide result")
require(elytra, ".runForLater(player", "Elytra cleanup is not entity-owned")
require(elytra, "onQuit(PlayerQuitEvent", "Elytra disconnect cleanup is missing")
forbid(elytra, "gliding::clear", "Elytra cleanup still clears every player's state globally")

bows = "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/listeners/SlimefunBowListener.java"
require(bows, "new ConcurrentHashMap<>()", "Projectile state is not concurrent")
require(bows, ".runForLater(", "Projectile cleanup is not entity-owned")

crafter = "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/listeners/AutoCrafterListener.java"
require(crafter, "GameRules.LIMITED_CRAFTING", "Modern limited-crafting gamerule is missing")
forbid(crafter, "GameRule.DO_LIMITED_CRAFTING", "Removed limited-crafting gamerule remains")

profiler = "src/main/java/io/github/thebusybiscuit/slimefun4/core/services/profiler/SlimefunProfiler.java"
for token in ("millisecondSamples", "nanosecondSamples", "samples == 0 ? 0"):
    require(profiler, token, f"Profiler invariant is missing: {token}")
forbid(profiler, "ticksPassed", "Profiler still shares one reset counter between metrics")

versions = "src/main/java/io/github/thebusybiscuit/slimefun4/core/commands/subcommands/VersionsCommand.java"
require(versions, "RECOMMENDED_JAVA_VERSION = 21", "Java recommendation is not current")
require(versions, '"Folia" : "Paper"', "Scheduler platform reporting is missing")
forbid(versions, "PaperLib", "Versions command still relies on PaperLib detection")

for path in (
    "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/electric/machines/AutoBrewer.java",
    "src/main/java/io/github/thebusybiscuit/slimefun4/utils/SlimefunUtils.java",
    "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/setup/SlimefunItemSetup.java",
):
    forbid(path, "PotionData", "Legacy PotionData API remains")
    forbid(path, "getBasePotionData", "Legacy PotionMeta getter remains")
    forbid(path, "setBasePotionData", "Legacy PotionMeta setter remains")

require(
    "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/magical/staves/WindStaff.java",
    "new FoodLevelChangeEvent(p, p.getFoodLevel() - 2, e.getItem())",
    "Wind Staff still uses the removed FoodLevelChangeEvent constructor",
)
require(
    "src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/magical/staves/StormStaff.java",
    "new FoodLevelChangeEvent(p, p.getFoodLevel() - 4, item)",
    "Storm Staff still uses the removed FoodLevelChangeEvent constructor",
)
read("src/test/java/io/github/thebusybiscuit/slimefun4/core/services/profiler/SlimefunProfilerAverageTest.java")

if errors:
    print("Part 4 verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("Part 4 Folia event-safety and Paper API static verification passed.")
