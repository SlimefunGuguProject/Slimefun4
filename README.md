# Slimefun Legacy — AlbionMC Fork (English)

This repository is an English-player-facing downstream build

**Slimefun Translate is not required.**

## What this fork changes

- Uses the latest imported Slimefun United & Slimefun 5 source as the technical base.
- Replaces hard-coded Chinese item names, lore, guide controls, messages, and logs
  with English equivalents.
- Defaults to `language: en` and `enable-translations: false`.
- Ignores language values previously saved on individual players while translations
  are disabled, preventing stale `zh-CN` or `zh-TW` preferences from taking over.
- Disables Slimefun's in-plugin auto-updater so it cannot replace this build with an
  untranslated upstream JAR.
- Preserves the Albion radiation event compatibility hook from the previous United
  source.
- Includes automated English verification, Gradle builds, and an upstream-sync
  workflow that opens a pull request for review.

Optional language files remain under `src/main/resources/languages/` for upstream
compatibility, but they are not used while translations are disabled.

## Requirements

- Java 21
- A compatible Paper server
- Gradle wrapper included in this repository

The imported upstream currently compiles against the Paper `1.21.11` API. Check the
upstream project and your server build before changing that dependency.

## Build

Linux/macOS:

```bash
chmod +x gradlew
python3 scripts/verify_english.py .
./gradlew clean build --no-daemon
```

Windows:

```powershell
python scripts/verify_english.py .
.\gradlew.bat clean build --no-daemon
```

The shaded plugin JAR is written to `build/libs/`.

## Server configuration

The included defaults are:

```yaml
options:
  auto-update: false
  language: en
  enable-translations: false
```

When updating an existing server, confirm those same values in
`plugins/Slimefun/config.yml`, then perform a full server restart.

### Existing Chinese item stacks

Minecraft stores an item's display name and lore inside the item stack. Items created
by an older Chinese build can therefore keep their old text even after installing this
fork. Newly created Slimefun items and newly opened guide content use English.

The source intentionally does not perform a global inventory rewrite because blindly
rebuilding every stored item could damage charge, ownership, enchantments, machine
state, addon metadata, or other persistent data.

## Automatic upstream synchronization

`.github/workflows/sync-gugu-upstream.yml` checks the Gugu `master` branch every six
hours and can also be run manually. The workflow:

1. Downloads a clean copy of the current Gugu source.
2. Applies `patches/albion-english.patch`.
3. Runs `scripts/verify_english.py`.
4. Builds the project with Java 21.
5. Opens or updates a pull request named **Sync latest SlimefunGuguProject updates**.

It never auto-merges upstream changes. This is intentional: if Gugu changes an item,
constructor, or localization path, the pull request provides a safe place to inspect
the result before deployment.

For the pull-request step to work, open your GitHub repository settings and enable:

- **Settings → Actions → General → Workflow permissions → Read and write permissions**
- **Allow GitHub Actions to create and approve pull requests**

If your default branch is not `main` or `master`, update the branch list in
`.github/workflows/build.yml` as needed.

## Manually stage an upstream update

Using a downloaded upstream ZIP:

```bash
scripts/sync_upstream.sh /path/to/Slimefun4-master.zip /tmp/slimefun-staged
```

Using a checked-out upstream directory:

```bash
scripts/sync_upstream.sh /path/to/Slimefun4 /tmp/slimefun-staged
```

The staged directory is verified before the script succeeds.

## Updating the English patch

After resolving an upstream conflict manually, regenerate the maintained patch:

```bash
scripts/regenerate_patch.sh /path/to/clean/Slimefun4
python3 scripts/verify_english.py .
./gradlew clean build --no-daemon
```

See [`ALBION_MERGE_NOTES.md`](ALBION_MERGE_NOTES.md) for the merge strategy and
verification details.

## License and attribution

This project retains the upstream GPL-3.0 license and its contributor history. It is
an unofficial downstream fork and is not a replacement for the original Slimefun or
Gugu projects.
