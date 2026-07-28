#!/usr/bin/env python3
"""Replace Slimefun add-on deprecation spam with one compatibility notice per add-on."""

from __future__ import annotations

import re
from pathlib import Path

TARGET = Path("src/main/java/io/github/thebusybiscuit/slimefun4/api/items/SlimefunItem.java")
IMPORT_LINE = "import java.util.concurrent.ConcurrentHashMap;"
SET_DECLARATION = (
    "    private static final Set<String> COMPATIBILITY_MODE_ADDONS = "
    "ConcurrentHashMap.newKeySet();"
)
MESSAGE = (
    '"Compatibility mode enabled. This addon uses supported legacy Slimefun APIs "\n'
    '                            + "and loaded normally."'
)


def replace_exact_once(text: str, old: str, new: str, label: str) -> tuple[str, bool]:
    count = text.count(old)
    if count > 1:
        raise RuntimeError(f"Refusing to replace {label}: found {count} matches")
    if count == 1:
        return text.replace(old, new, 1), True
    return text, False


def main() -> None:
    if not TARGET.is_file():
        raise SystemExit(f"Missing target source file: {TARGET}")

    text = TARGET.read_text(encoding="utf-8")
    original = text

    # Support both untouched master and the earlier quiet-startup patch.
    text = text.replace(
        "private static final Set<String> REPORTED_LEGACY_APIS = ConcurrentHashMap.newKeySet();",
        "private static final Set<String> COMPATIBILITY_MODE_ADDONS = ConcurrentHashMap.newKeySet();",
    )
    text = text.replace("logSupportedLegacyApi(c);", "enableCompatibilityMode();")
    text = text.replace("logSupportedLegacyApi(parent);", "enableCompatibilityMode();")

    old_legacy_method = re.compile(
        r"\n    private void logSupportedLegacyApi\(@Nonnull Class<\?> legacyType\) \{.*?\n    \}\n",
        re.DOTALL,
    )
    text = old_legacy_method.sub("\n", text, count=1)

    if IMPORT_LINE not in text:
        marker = "import java.util.Set;\n"
        if marker not in text:
            raise RuntimeError("Could not locate java.util.Set import")
        text = text.replace(marker, marker + IMPORT_LINE + "\n", 1)

    if "COMPATIBILITY_MODE_ADDONS" not in text:
        class_marker = "public class SlimefunItem implements Placeable {\n"
        if class_marker not in text:
            raise RuntimeError("Could not locate SlimefunItem class declaration")
        text = text.replace(class_marker, class_marker + SET_DECLARATION + "\n\n", 1)

    inherited_warning = '''                warn("The inherited Class \\\""
                        + c.getName()
                        + "\\\" has been deprecated. Check the documentation for more details!");'''
    interface_warning = '''                    warn("The implemented Interface \\\""
                            + parent.getName()
                            + "\\\" has been deprecated. Check the documentation for more details!");'''

    text, inherited_replaced = replace_exact_once(
        text, inherited_warning, "                enableCompatibilityMode();", "inherited-class warning"
    )
    text, interface_replaced = replace_exact_once(
        text, interface_warning, "                    enableCompatibilityMode();", "interface warning"
    )

    text = text.replace(
        "If a {@link Deprecated} element was found, a warning message will be printed.",
        "If a {@link Deprecated} element is found, compatibility mode is enabled for the addon.",
    )
    text = text.replace(
        "// Send out deprecation warnings for any classes or interfaces",
        "// Detect legacy API use and enable quiet compatibility mode",
    )
    text = text.replace(
        " * Make developers or at least Server admins aware that an Item\n"
        " * is using a deprecated ItemHandler",
        " * Detect legacy ItemHandlers and enable quiet compatibility mode",
    )

    if "private void enableCompatibilityMode()" not in text:
        insertion_marker = "\n    /**\n     * This method will set the {@link Research} of this {@link SlimefunItem}."
        if insertion_marker not in text:
            raise RuntimeError("Could not locate method insertion point")
        method = f'''\n    private void enableCompatibilityMode() {{
        String addonName = addon.getName();

        if (COMPATIBILITY_MODE_ADDONS.add(addonName)) {{
            addon.getLogger().log(
                    Level.INFO,
                    {MESSAGE});
        }}
    }}
'''
        text = text.replace(insertion_marker, method + insertion_marker, 1)

    # Idempotency and safety checks.
    required = [
        IMPORT_LINE,
        "COMPATIBILITY_MODE_ADDONS",
        "private void enableCompatibilityMode()",
        "Compatibility mode enabled.",
    ]
    for token in required:
        if token not in text:
            raise RuntimeError(f"Required compatibility-mode token missing: {token}")

    forbidden = [
        'warn("The inherited Class',
        'warn("The implemented Interface',
        "logSupportedLegacyApi(",
        "REPORTED_LEGACY_APIS",
    ]
    for token in forbidden:
        if token in text:
            raise RuntimeError(f"Old deprecation-warning path remains: {token}")

    if text.count("private void enableCompatibilityMode()") != 1:
        raise RuntimeError("Compatibility helper must exist exactly once")
    if text.count(SET_DECLARATION) != 1:
        raise RuntimeError("Compatibility add-on set must exist exactly once")

    if text == original:
        print("Compatibility-mode source is already up to date.")
        return

    TARGET.write_text(text, encoding="utf-8", newline="\n")
    print(f"Updated {TARGET}")
    print(f"Replaced inherited warning: {inherited_replaced}")
    print(f"Replaced interface warning: {interface_replaced}")


if __name__ == "__main__":
    main()
