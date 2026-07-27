#!/usr/bin/env python3
"""Static safety checks for the Gugu upstream synchronization workflow."""
from __future__ import annotations

import sys
from pathlib import Path


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    workflow = root / ".github/workflows/sync-gugu-upstream.yml"
    script = root / "scripts/sync_upstream.sh"
    docs = root / "GUGU_UPSTREAM_SYNC.md"

    problems: list[str] = []
    for path in (workflow, script, docs):
        if not path.is_file():
            problems.append(f"missing {path.relative_to(root)}")

    if problems:
        for problem in problems:
            print(f"ERROR: {problem}", file=sys.stderr)
        return 1

    workflow_text = workflow.read_text(encoding="utf-8")
    script_text = script.read_text(encoding="utf-8")

    forbidden = ("rsync -a --delete", "git checkout --theirs", "git merge -X theirs", "git reset --hard upstream")
    for needle in forbidden:
        if needle in workflow_text or needle in script_text:
            problems.append(f"unsafe sync operation found: {needle}")

    required_workflow = (
        "fetch-depth: 0",
        "scripts/sync_upstream.sh",
        "--draft",
        "verify_english.py",
        "verify_part2.py",
        "check_api_annotations.py",
        "force-with-lease",
    )
    for needle in required_workflow:
        if needle not in workflow_text and needle not in script_text:
            problems.append(f"missing sync safeguard: {needle}")

    required_script = ("git merge --no-ff --no-edit", "git diff --name-only --diff-filter=U", "git merge --abort")
    for needle in required_script:
        if needle not in script_text:
            problems.append(f"missing merge safeguard: {needle}")

    if problems:
        for problem in problems:
            print(f"ERROR: {problem}", file=sys.stderr)
        return 1

    print("Gugu sync safety verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
