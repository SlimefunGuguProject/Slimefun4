# Slimefun Legacy — Third Maintenance Release

## Storage Safety & Data Modernization

This release modernizes Slimefun Legacy's persistent ItemStack storage while retaining the older public String-based methods used by existing addons.

## Main changes

- Raises the database schema from version 2 to version 3.
- Stores backpack, block-menu, and universal inventory items as versioned binary ItemStack data.
- Uses Paper's native ItemStack binary format for new records.
- Reads and migrates legacy Base64/Bukkit object-stream items in place.
- Preserves legacy skull profiles, texture properties, old ItemMeta payloads, and persistent data where Paper can recover them.
- Adds binary inventory columns for MySQL, SQLite, and PostgreSQL.
- Adds a retry-safe migration that publishes schema version 3 only after every migration step succeeds.
- Defers universal-block location resolution when its world is temporarily unavailable.
- Corrects PostgreSQL inventory slot types and metadata SQL portability.

## Addon compatibility

The following descriptors remain available:

- `DataUtils.serializeItemStack(ItemStack): String`
- `DataUtils.deserializeItemStack(String): ItemStack`
- `RecordSet.getAll(): Map<FieldKey, String>`
- `RecordSet.get(FieldKey): String`
- `SqlUtils.buildKvStr(FieldKey, String): String`
- `SqlUtils.toSqlValStr(FieldKey, String): String`

New core storage code uses binary overloads internally. Older addons can continue consuming Base64 String views without being required to recompile for this release.

## Database safety

**Back up all Slimefun databases before the first startup with this release.**

The first startup upgrades inventory storage to schema 3. The version record is written only after the migration succeeds. Failed item rows prevent version publication so the migration can be inspected and retried.

MySQL can implicitly commit table-alter operations. Even though the row migration and schema-version publication are guarded, restoring a backup remains the only supported downgrade path.

Do not downgrade to a schema-2 build against a database that has been opened by this release. Restore the pre-upgrade backup first.

## Recommended upgrade procedure

1. Stop the server completely.
2. Copy the complete Slimefun plugin data directory and every configured SQL database.
3. Keep the backup outside the live server directory.
4. Install the new JAR and start the server once with players offline.
5. Wait for the database migration completion message.
6. Review the log for any item migration failures.
7. Test backpacks, Cargo storage, machine inventories, universal storage, custom skulls, and addon items.
8. Reopen the server only after those checks pass.

## Validation included

- Public storage API descriptor tests
- SQLite schema-2 to schema-3 end-to-end migration test
- Migration idempotency test
- Transaction rollback and version-publication ordering tests
- Optional copied-production SQLite migration test
- Legacy skull profile repair test
- Missing-world location deferral test
- MySQL, SQLite, and PostgreSQL schema invariants
- Part 1 and Part 2 regression checks
