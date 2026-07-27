package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.ItemStackDataCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

class DatabasePatchV3RealDatabaseTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(System.getProperty("slimefun.realDatabase") != null);
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void migratesAndDeserializesEveryInventoryItem() throws Exception {
        var source = Path.of(System.getProperty("slimefun.realDatabase"));
        var database = tempDir.resolve("block-storage.db");
        Files.copy(source, database);

        var logger = Logger.getLogger(getClass().getName());
        var failures = new ArrayList<String>();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getThrown() != null && failures.size() < 10) {
                    failures.add(record.getMessage() + ": " + describe(record.getThrown()));
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });

        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                var statement = connection.createStatement()) {
            var patch = new DatabasePatchV3(() -> logger);
            try {
                patch.patch(statement, new SqliteConfig(database.toString(), 1));
            } catch (SQLException exception) {
                fail(exception.getMessage() + System.lineSeparator() + String.join(System.lineSeparator(), failures));
            }

            for (var table : List.of("block_inventory", "universal_inventory")) {
                try (var result = statement.executeQuery("SELECT i_item, typeof(i_item) FROM " + table)) {
                    var count = 0;
                    while (result.next()) {
                        assertEquals("blob", result.getString(2));
                        var item = assertDoesNotThrow(() -> ItemStackDataCodec.deserialize(result.getBytes(1)));
                        assertTrue(!item.getType().isAir());
                        count++;
                    }
                    assertTrue(count > 0);
                }
            }
        }
    }

    private String describe(Throwable throwable) {
        var result = new StringBuilder();
        for (var cause = throwable; cause != null; cause = cause.getCause()) {
            if (!result.isEmpty()) {
                result.append(" -> ");
            }
            result.append(cause.getClass().getName()).append(": ").append(cause.getMessage());
        }
        return result.toString();
    }
}
