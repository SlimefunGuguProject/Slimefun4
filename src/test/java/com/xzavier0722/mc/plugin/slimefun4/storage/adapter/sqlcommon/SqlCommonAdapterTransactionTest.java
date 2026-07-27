package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.patch.DatabasePatch;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class SqlCommonAdapterTransactionTest {
    @Test
    void rollsBackPatchAndRestoresAutoCommitOnFailure() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                var setup = connection.createStatement()) {
            setup.execute("CREATE TABLE transaction_probe (value TEXT NOT NULL)");
            setup.execute("CREATE TABLE table_metadata (m_key TEXT PRIMARY KEY, m_value TEXT NOT NULL)");

            var patch = new DatabasePatch(3) {
                @Override
                public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
                    stmt.execute("INSERT INTO transaction_probe VALUES ('must rollback')");
                    throw new SQLException("forced migration failure");
                }
            };

            assertThrows(
                    SQLException.class,
                    () -> SqlCommonAdapter.executePatchTransaction(
                            connection, patch, new SqliteConfig(":memory:", 1)));
            assertTrue(connection.getAutoCommit());

            try (var result = setup.executeQuery("SELECT COUNT(*) FROM transaction_probe")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (var result = setup.executeQuery("SELECT COUNT(*) FROM table_metadata")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        }
    }

    @Test
    void commitsPatchBeforePublishingSchemaVersion() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                var setup = connection.createStatement()) {
            setup.execute("CREATE TABLE transaction_probe (value TEXT NOT NULL)");
            setup.execute("CREATE TABLE table_metadata (m_key TEXT PRIMARY KEY, m_value TEXT NOT NULL)");

            var patch = new DatabasePatch(3) {
                @Override
                public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
                    stmt.execute("INSERT INTO transaction_probe VALUES ('committed')");
                }
            };

            SqlCommonAdapter.executePatchTransaction(connection, patch, new SqliteConfig(":memory:", 1));
            assertTrue(connection.getAutoCommit());

            try (var result = setup.executeQuery("SELECT value FROM transaction_probe")) {
                assertTrue(result.next());
                assertEquals("committed", result.getString(1));
            }
            try (var result = setup.executeQuery("SELECT m_value FROM table_metadata WHERE m_key='version'")) {
                assertTrue(result.next());
                assertEquals("3", result.getString(1));
            }
        }
    }
}
