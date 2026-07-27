#!/usr/bin/env python3
"""Static invariants for the Part 3 storage-safety release."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def read(path: str) -> str:
    target = ROOT / path
    if not target.is_file():
        errors.append(f"Missing required file: {path}")
        return ""
    return target.read_text(encoding="utf-8")


def require(path: str, text: str, description: str) -> None:
    content = read(path)
    if text not in content:
        errors.append(f"{description}: {path}")


def forbid(path: str, text: str, description: str) -> None:
    content = read(path)
    if text in content:
        errors.append(f"{description}: {path}")


require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/IDataSourceAdapter.java",
    "int DATABASE_VERSION = 3;",
    "Database schema version is not 3",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/ItemStackDataCodec.java",
    "private static final byte[] FORMAT_V2 = {'S', 'F', '2', 0};",
    "Versioned ItemStack marker is missing",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/DataUtils.java",
    "public static String serializeItemStack(ItemStack itemStack)",
    "Legacy String serializer descriptor is missing",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/DataUtils.java",
    "public static byte[] serializeItemStackBytes(ItemStack itemStack)",
    "Binary serializer is missing",
)
forbid(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/DataUtils.java",
    "public final class DataUtils",
    "DataUtils was made final and would break subclassing compatibility",
)
forbid(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/DataUtils.java",
    "private DataUtils()",
    "DataUtils public no-argument constructor compatibility was removed",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/sqlcommon/SqlUtils.java",
    "public static String toSqlValStr(FieldKey key, String val)",
    "Legacy SqlUtils String descriptor is missing",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/common/RecordSet.java",
    "private final Map<FieldKey, Object> data;",
    "RecordSet does not retain binary values",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/common/RecordSet.java",
    "public Map<FieldKey, String> getAll()",
    "Legacy RecordSet String view is missing",
)
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/common/RecordSet.java",
    "public Map<FieldKey, Object> getAllValues()",
    "Binary RecordSet view is missing",
)

for adapter, sql_type in (
    ("mysql/MysqlAdapter.java", "MEDIUMBLOB NOT NULL"),
    ("sqlite/SqliteAdapter.java", "BLOB NOT NULL"),
    ("postgresql/PostgreSqlAdapter.java", "BYTEA NOT NULL"),
):
    path = f"src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/{adapter}"
    content = read(path)
    if content.count(sql_type) < 3:
        errors.append(f"All three inventory tables are not binary in {path}")
    if "var data = item.getAll();" in content:
        errors.append(f"Legacy String storage view is still used by {path}")

forbidden_postgres = read(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/postgresql/PostgreSqlAdapter.java"
)
if "TINYINT UNSIGNED" in forbidden_postgres:
    errors.append("PostgreSQL adapter still contains MySQL-only TINYINT UNSIGNED")

transaction = read(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/sqlcommon/SqlCommonAdapter.java"
)
patch_index = transaction.find("patch.patch(stmt, config);")
version_index = transaction.find("patch.updateVersion(stmt, config);")
commit_index = transaction.find("conn.commit();")
if min(patch_index, version_index, commit_index) < 0 or not (patch_index < version_index < commit_index):
    errors.append("Database patch, version publication, and commit are not ordered safely")
for token in ("conn.setAutoCommit(false);", "conn.rollback();", "conn.setAutoCommit(originalAutoCommit);"):
    if token not in transaction:
        errors.append(f"Transactional migration safeguard is missing: {token}")

migration = read(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/patch/DatabasePatchV3.java"
)
if "private static void migrateInventoryTable" in migration:
    errors.append("DatabasePatchV3 migration cannot be static because it uses the instance logger")

for token in (
    "DataScope.BACKPACK_INVENTORY",
    "DataScope.BLOCK_INVENTORY",
    "DataScope.UNIVERSAL_INVENTORY",
    "isBinaryColumn",
    "ItemStackDataCodec.isLegacy",
    "ItemStackDataCodec.deserialize",
    "ItemStackDataCodec.serialize",
):
    if token not in migration:
        errors.append(f"Schema-3 migration invariant is missing: {token}")

controller = read(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/controller/BlockDataController.java"
)
if "ubd.setLastPresent(LocationUtils.toLocation(lStr));" in controller:
    errors.append("Universal block locations are still resolved eagerly during data load")
require(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/controller/SlimefunUniversalBlockData.java",
    "Keep the stored location string intact until the world becomes available.",
    "Missing-world data deferral is absent",
)

for test in (
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/PublicStorageApiCompatibilityTest.java",
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/patch/DatabasePatchV3E2ETest.java",
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/patch/DatabasePatchV3RealDatabaseTest.java",
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/sqlcommon/SqlCommonAdapterTransactionTest.java",
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/LegacySkullProfileDataFixerTest.java",
    "src/test/java/com/xzavier0722/mc/plugin/slimefun4/storage/util/MissingWorldLocationTest.java",
):
    read(test)

postgres = read(
    "src/main/java/com/xzavier0722/mc/plugin/slimefun4/storage/adapter/postgresql/PostgreSqlAdapter.java"
)
if "VALUES ('{3}', '{4}')" in postgres:
    errors.append("PostgreSQL metadata insert still uses MessageFormat-quoted placeholders")

if errors:
    print("Part 3 verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("Part 3 storage-safety static verification passed.")
