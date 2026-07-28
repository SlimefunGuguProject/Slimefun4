# Slimefun Legacy compatibility-mode console cleanup

This drop-in replaces repeated runtime deprecation warnings from supported legacy addon APIs with one informational notice per addon:

```text
Compatibility mode enabled. This addon uses supported legacy Slimefun APIs and loaded normally.
```

## Behavior

- Logs at `INFO`, not `WARNING`.
- Prints at most once per addon during startup.
- Keeps legacy API detection enabled.
- Keeps real Slimefun warnings and errors unchanged.
- Removes the old automatic addon bug-report link from this compatibility path.
- Supports both untouched current master and the earlier quiet-startup patch.

## Use

1. Extract this ZIP into the root of the Slimefun Legacy repository.
2. Commit the new workflow and script to GitHub.
3. Open **Actions** → **Apply Compatibility Mode Enabled Fix** → **Run workflow**.
4. The workflow updates `SlimefunItem.java`, formats it, compiles the main source, and commits the result to `master`.

The fix affects server startup/runtime addon registration messages. Java compiler deprecation notices shown while building are separate and are not server-console messages.
