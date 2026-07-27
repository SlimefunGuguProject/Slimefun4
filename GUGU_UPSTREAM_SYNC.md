# Gugu Upstream Sync

Slimefun Legacy intentionally diverges from SlimefunGuguProject/Slimefun4. It contains English-only behavior, addon compatibility work, the Stability Release, the Second Maintenance Release, and the Third Maintenance Release. Upstream changes should therefore be merged and reviewed, not copied over the fork.

## Why the July 2026 storage update matters

The current upstream comparison contains the database schema v3 storage work:

- ItemStacks are stored as binary Paper serialization instead of legacy Java-object/Base64 text.
- Existing database rows are migrated while retaining legacy deserialization compatibility.
- MySQL, PostgreSQL, and SQLite inventory columns move to binary column types.
- Database migration and schema-version updates are performed transactionally.
- Universal block data remains unresolved when its world is not loaded, then resolves after that world becomes available.
- Storage API compatibility and migration tests are added.

This is valuable for modern Paper item metadata and database reliability, but it is not a low-risk cosmetic update. Once a production database has migrated to schema version 3, downgrading to a build that only understands schema version 2 is unsafe without restoring the pre-upgrade database backup.

## Integrated upstream baseline

Part 3 manually integrates the Gugu storage branch through upstream merge commit `ece7368e1d0b40bc95c63d2796117794fcaf190e`. The file `.gugu-upstream-base` records that revision.

When the source changes are uploaded without their original upstream Git parent, the sync script first creates an **ours merge** to connect that recorded upstream revision to the fork history without changing any source files. It then performs a normal merge of only commits newer than the recorded baseline. After a successful sync, the marker advances to the new upstream commit.

Do not delete or manually change `.gugu-upstream-base` unless intentionally re-establishing the upstream integration point. The workflow refuses rewritten or unrelated upstream history rather than guessing.

## Safe workflow

`.github/workflows/sync-gugu-upstream.yml` performs a real Git merge into `automation/gugu-upstream-sync` and opens or updates a **draft pull request**.

It deliberately does not:

- replace the source tree with `rsync --delete`;
- auto-merge the pull request;
- choose upstream versions of conflicted files;
- bypass the English-only guard;
- push anything when Git reports merge conflicts.

A clean merge must pass:

- `scripts/verify_english.py`;
- `scripts/verify_part2.py`;
- `scripts/verify_part3.py`;
- `scripts/verify_gugu_sync.py`;
- `scripts/check_api_annotations.py`;
- Spotless;
- the test suite;
- the full Gradle build.

## Running it

1. Push the completed Part 3 source to the repository's default branch.
2. Open **Actions → Sync Gugu Upstream → Run workflow**.
3. Leave `upstream_ref` as `master` unless testing a specific upstream branch.
4. Review the generated draft pull request and workflow artifact.
5. Resolve any English wording, scheduler ownership, API compatibility, or addon compatibility issues in the PR branch.
6. Test the resulting JAR against a copy of the production Slimefun database.
7. Merge only after the backup/restore test succeeds.

## Production database procedure

Before the first server boot using the schema-v3 build:

1. Stop the server cleanly.
2. Back up the entire `plugins/Slimefun` directory.
3. Back up the SQLite file or external MySQL/PostgreSQL database independently.
4. Start a staging copy and allow the migration to complete.
5. Verify backpacks, block inventories, universal storage, skull/profile metadata, cargo, and addon machines.
6. Keep the pre-migration backup until the new build has run successfully for several restarts.

Do not test this migration for the first time on the live database.

## Local use

From a clean Git checkout:

```bash
scripts/sync_upstream.sh master automation/gugu-upstream-sync
```

To include the full local validation/build:

```bash
GUGU_SYNC_BUILD=1 scripts/sync_upstream.sh master automation/gugu-upstream-sync
```

If a merge conflict occurs locally, the script leaves the conflict for manual resolution. In GitHub Actions, the workflow aborts the merge and uploads the conflict report instead.
