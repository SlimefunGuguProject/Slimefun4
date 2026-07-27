#!/usr/bin/env python3
"""Static release checks for the Slimefun Legacy second maintenance release."""

from __future__ import annotations

from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
MAIN = ROOT / "src/main"
JAVA = ROOT / "src/main/java"
failures: list[str] = []


def require(path: Path, needle: str, message: str) -> None:
    text = path.read_text(encoding="utf-8")
    if needle not in text:
        failures.append(message)


messages = MAIN / "resources/languages/en/messages.yml"
require(messages, "connected: '&7Connected: &2✔'", "Connected Cargo message is missing")
require(messages, "not-connected: '&7Connected: &4✕'", "Disconnected Cargo message is missing")
require(
    messages,
    "connection-status-connected: '&7Connected: &2✔'",
    "Stale-language-safe connected message is missing",
)
require(
    messages,
    "connection-status-disconnected: '&7Connected: &4✕'",
    "Stale-language-safe disconnected message is missing",
)

for source in MAIN.rglob("*"):
    if source.is_file() and source.suffix in {".java", ".yml", ".yaml", ".properties"}:
        text = source.read_text(encoding="utf-8", errors="replace").lower()
        if "connectstate" in text or "connectedstate" in text:
            failures.append(f"Legacy connector state text remains in {source.relative_to(ROOT)}")

scheduler_violations: list[str] = []
for source in JAVA.rglob("*.java"):
    relative = source.relative_to(ROOT).as_posix()
    text = source.read_text(encoding="utf-8")
    if relative.endswith("implementation/scheduling/PaperScheduler.java"):
        continue

    for line_number, line in enumerate(text.splitlines(), start=1):
        if "Bukkit.getScheduler()" in line:
            if relative.endswith("implementation/Slimefun.java") and "cancelTasks(this)" in line:
                continue
            scheduler_violations.append(f"{relative}:{line_number}: {line.strip()}")
        if "new BukkitRunnable" in line or re.search(r"\.runTask(?:Later|Timer|Asynchronously)?\(", line):
            scheduler_violations.append(f"{relative}:{line_number}: {line.strip()}")

if scheduler_violations:
    failures.append("Direct scheduler calls remain outside PaperScheduler:\n  " + "\n  ".join(scheduler_violations))

slimefun = JAVA / "io/github/thebusybiscuit/slimefun4/implementation/Slimefun.java"
require(slimefun, "runSyncAt(", "Location-owned compatibility scheduler helper is missing")
require(slimefun, "runSyncFor(", "Entity-owned compatibility scheduler helper is missing")
require(slimefun, "new LegacyBukkitTask", "Legacy BukkitTask adapter is not wired")


scheduler_api = JAVA / "io/github/thebusybiscuit/slimefun4/core/services/scheduling/SlimefunScheduler.java"
require(scheduler_api, "Runnable retired", "Entity-retirement scheduler callbacks are missing")

paper_scheduler = JAVA / "io/github/thebusybiscuit/slimefun4/implementation/scheduling/PaperScheduler.java"
require(paper_scheduler, "handle.retire(retired)", "Paper entity retirement cleanup is not wired")

item_doctor = JAVA / "io/github/thebusybiscuit/slimefun4/core/services/stability/ItemDoctorService.java"
require(item_doctor, "pendingOwnedWork", "Item Doctor owned-work completion tracking is missing")
require(item_doctor, "runFor(entity, task, retired)", "Item Doctor entity retirement cleanup is missing")

research_task = JAVA / "io/github/thebusybiscuit/slimefun4/api/researches/PlayerResearchTask.java"
require(research_task, "clearResearchState", "Research unlock retirement cleanup is missing")

block_ticker = JAVA / "me/mrCookieSlime/Slimefun/Objects/handlers/BlockTicker.java"
require(
    block_ticker,
    "tick(Block b, SlimefunItem item, ASlimefunDataContainer data)",
    "Storage-neutral BlockTicker overload is missing",
)

energy = JAVA / "io/github/thebusybiscuit/slimefun4/core/attributes/EnergyNetComponent.java"
for signature in (
    "setCharge(\n            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data)",
    "addCharge(\n            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data)",
    "removeCharge(\n            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data)",
):
    require(energy, signature, f"Modern energy overload is missing: {signature.split('(')[0]}")
require(energy, "long capacity = getCapacityLong();", "Long-capacity energy mutations do not use getCapacityLong()")

for integration in (
    JAVA / "io/github/thebusybiscuit/slimefun4/implementation/items/cargo/AbstractCargoNode.java",
    JAVA / "me/mrCookieSlime/Slimefun/Objects/SlimefunItem/interfaces/InventoryBlock.java",
):
    require(integration, "ProtectionCompatibility.isAllowed", f"Protection compatibility policy not used by {integration.name}")

annotation_check = subprocess.run(
    [sys.executable, str(ROOT / "scripts/check_api_annotations.py")],
    cwd=ROOT,
    text=True,
    capture_output=True,
    check=False,
)
if annotation_check.returncode != 0:
    failures.append(annotation_check.stdout + annotation_check.stderr)

if failures:
    for failure in failures:
        print(f"ERROR: {failure}", file=sys.stderr)
    raise SystemExit(1)

print("Second maintenance release static verification passed.")
