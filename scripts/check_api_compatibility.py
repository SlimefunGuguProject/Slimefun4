#!/usr/bin/env python3
"""Fail when a released Slimefun Legacy public JVM signature disappears."""
from __future__ import annotations

import argparse
import subprocess
import tempfile
import zipfile
from pathlib import Path

PUBLIC_PREFIXES = (
    "io/github/thebusybiscuit/slimefun4/api/",
    "io/github/thebusybiscuit/slimefun4/core/attributes/",
    "me/mrCookieSlime/Slimefun/Objects/handlers/",
    "me/mrCookieSlime/Slimefun/api/",
)


def public_signatures(jar: Path) -> set[str]:
    signatures: set[str] = set()
    with zipfile.ZipFile(jar) as archive:
        classes = sorted(
            name[:-6].replace("/", ".")
            for name in archive.namelist()
            if name.endswith(".class")
            and "$" not in name
            and name.startswith(PUBLIC_PREFIXES)
        )

    for class_name in classes:
        result = subprocess.run(
            ["javap", "-public", "-classpath", str(jar), class_name],
            check=False,
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            continue
        for raw in result.stdout.splitlines():
            line = " ".join(raw.strip().split())
            if not line or line.startswith("Compiled from") or line == "}" or line.endswith("{"):
                continue
            if line.startswith(("public ", "protected ")):
                signatures.add(f"{class_name} :: {line}")
    return signatures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--allowlist", type=Path, default=Path("scripts/api-removal-allowlist.txt"))
    args = parser.parse_args()

    baseline = public_signatures(args.baseline)
    candidate = public_signatures(args.candidate)
    allowed = set()
    if args.allowlist.exists():
        allowed = {
            line.strip()
            for line in args.allowlist.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        }

    removed = sorted((baseline - candidate) - allowed)
    report = Path("build/reports/api-compatibility.txt")
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(
        "Removed public signatures:\n" + ("\n".join(removed) if removed else "None") + "\n",
        encoding="utf-8",
    )

    if removed:
        print(f"Detected {len(removed)} unapproved public API removal(s):")
        print("\n".join(removed))
        return 1

    print(f"API compatibility passed ({len(baseline)} baseline signatures checked).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
