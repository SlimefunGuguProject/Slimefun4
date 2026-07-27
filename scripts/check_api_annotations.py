#!/usr/bin/env python3
"""Verify that public top-level types in compatibility-protected packages are classified."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src/main/java"
PUBLIC_PREFIXES = (
    "io/github/thebusybiscuit/slimefun4/api",
    "io/github/thebusybiscuit/slimefun4/core/attributes",
    "io/github/thebusybiscuit/slimefun4/core/services/scheduling",
    "me/mrCookieSlime/Slimefun/Objects/handlers",
    "me/mrCookieSlime/Slimefun/api",
)
DECLARATION = re.compile(
    r"^public\s+(?:(?:abstract|final|sealed|non-sealed|static)\s+)*"
    r"(?:class|interface|enum|record)\s+\w+",
    re.MULTILINE,
)
EXEMPT_FILES = {"SlimefunAPI.java", "SlimefunInternal.java"}


def main() -> int:
    missing: list[Path] = []
    conflicting: list[Path] = []

    for prefix in PUBLIC_PREFIXES:
        for source in sorted((SOURCE_ROOT / prefix).rglob("*.java")):
            text = source.read_text(encoding="utf-8")
            if not DECLARATION.search(text) or source.name in EXEMPT_FILES:
                continue

            has_api = "@SlimefunAPI" in text
            has_internal = "@SlimefunInternal" in text
            if not has_api and not has_internal:
                missing.append(source.relative_to(ROOT))
            elif has_api and has_internal:
                conflicting.append(source.relative_to(ROOT))

    if missing:
        print("Public API types missing @SlimefunAPI or @SlimefunInternal:")
        for source in missing:
            print(f"  - {source}")

    if conflicting:
        print("Public types cannot be both @SlimefunAPI and @SlimefunInternal:")
        for source in conflicting:
            print(f"  - {source}")

    if missing or conflicting:
        return 1

    print("API annotation inventory is complete for all compatibility-protected packages.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
