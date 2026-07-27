package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.ItemStackDataCodec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class DatabasePatchV3E2ETest {
    private static final Logger LOGGER = Logger.getLogger(DatabasePatchV3E2ETest.class.getName());

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void migratesLegacyBase64TextToBinaryPaperItemStackAndIsIdempotent() throws Exception {
        var expected = new ItemStack(Material.DIAMOND_SWORD);
        expected.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
        var meta = expected.getItemMeta();
        meta.setDisplayName("Migration Test Sword");
        meta.setLore(List.of("Legacy object stream", "Metadata must be preserved"));
        expected.setItemMeta(meta);
        var legacyText = serializeLegacy(expected);

        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                var statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE backpack_inventory (b_id INTEGER NOT NULL, i_slot INTEGER NOT NULL, i_item TEXT NOT NULL, PRIMARY KEY (b_id, i_slot))");
            try (var insert = connection.prepareStatement("INSERT INTO backpack_inventory VALUES (?, ?, ?)")) {
                insert.setInt(1, 42);
                insert.setInt(2, 3);
                insert.setString(3, legacyText);
                insert.executeUpdate();
            }

            var patch = new DatabasePatchV3(() -> LOGGER);
            var config = new SqliteConfig(":memory:", 1);
            patch.patch(statement, config);

            var firstMigration = readStoredItem(statement);
            assertEquals("blob", firstMigration.storageType());
            var binary = assertInstanceOf(byte[].class, firstMigration.value());
            assertTrue(binary.length < legacyText.getBytes(StandardCharsets.US_ASCII).length);
            assertTrue(expected.isSimilar(ItemStackDataCodec.deserialize(binary)));

            patch.patch(statement, config);
            var secondMigration = readStoredItem(statement);
            assertEquals("blob", secondMigration.storageType());
            assertArrayEquals(binary, assertInstanceOf(byte[].class, secondMigration.value()));
        }
    }

    @Test
    void rejectsMalformedLegacyRowsSoTheMigrationCanBeRetried() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                var statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE backpack_inventory (b_id INTEGER NOT NULL, i_slot INTEGER NOT NULL, i_item TEXT NOT NULL, PRIMARY KEY (b_id, i_slot))");
            statement.execute("INSERT INTO backpack_inventory VALUES (42, 3, 'not-valid-base64')");

            var patch = new DatabasePatchV3(() -> LOGGER);
            assertThrows(java.sql.SQLException.class, () -> patch.patch(statement, new SqliteConfig(":memory:", 1)));

            try (var result = statement.executeQuery(
                    "SELECT i_item, typeof(i_item) FROM backpack_inventory WHERE b_id=42 AND i_slot=3")) {
                assertTrue(result.next());
                assertEquals("not-valid-base64", result.getString(1));
                assertEquals("text", result.getString(2));
            }
        }
    }

    private StoredItem readStoredItem(java.sql.Statement statement) throws Exception {
        try (var result = statement.executeQuery(
                "SELECT i_item, typeof(i_item) AS storage_type FROM backpack_inventory WHERE b_id=42 AND i_slot=3")) {
            assertTrue(result.next());
            return new StoredItem(result.getObject("i_item"), result.getString("storage_type"));
        }
    }

    private String serializeLegacy(ItemStack itemStack) throws Exception {
        try (var bytes = new ByteArrayOutputStream();
                var output = new BukkitObjectOutputStream(bytes)) {
            output.writeObject(itemStack);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }

    private record StoredItem(Object value, String storageType) {}
}
