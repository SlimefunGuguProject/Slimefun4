# Part 3 Validation — Storage Safety & Data Modernization

## Automated checks

Run the complete verification set:

```bash
python3 scripts/verify_english.py .
python3 scripts/verify_chunk_load_threading.py .
python3 scripts/check_api_annotations.py
python3 scripts/verify_part2.py .
python3 scripts/verify_part3.py .
./gradlew spotlessCheck clean build --no-daemon
```

The Gradle build runs MockBukkit and SQLite tests covering API compatibility, legacy ItemStack migration, transaction rollback, migration idempotency, legacy skull repair, and missing-world location handling.

## Optional copied-production SQLite test

Never point this test at the live server database. Stop the server and provide a copied database file:

```bash
./gradlew test -PslimefunRealDatabase=/absolute/path/to/copied-block-storage.db --no-daemon
```

The test creates another temporary copy before migration. It verifies that stored block and universal inventory items become SQLite BLOB values and deserialize successfully.

## Manual server checks

After backing up and upgrading:

- Confirm the startup log reports database schema 3.
- Open old backpacks and verify every slot and custom item.
- Open Slimefun machines and Cargo-managed inventories.
- Check addon items with custom PDC, lore, enchantments, skull textures, and item components.
- Restart the server and confirm migrated items still load.
- Temporarily load a world containing universal storage after Slimefun starts and verify its stored position resolves when the world becomes available.
- Review the database migration log for `failed=0` on every inventory table.

## Rollback rule

A schema-3 database must not be used with an older schema-2 build. To roll back the plugin, stop the server and restore the complete pre-upgrade database backup.
