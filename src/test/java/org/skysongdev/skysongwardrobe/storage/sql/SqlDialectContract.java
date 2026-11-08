package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Runs each dialect's statements on the real database it is for, so the SQL is known to work. */
abstract class SqlDialectContract {
    private SqlDatabase database;
    private String table;

    /** An empty database of the dialect's kind. */
    abstract SqlDatabase emptyDatabase() throws Exception;

    @BeforeEach
    void createTable() throws Exception {
        database = emptyDatabase();
        table = database.table("dialect_test");
        execute("CREATE TABLE " + table + " (k VARCHAR(16) NOT NULL, n INT NOT NULL, v VARCHAR(16) NOT NULL, b "
                + database.dialect().largeBlobType() + ", PRIMARY KEY (k, n))");
    }

    @Test
    void upsertInsertsANewRow() throws SQLException {
        upsert("a", 1, "first");

        assertEquals(Optional.of("first"), value("a", 1));
    }

    @Test
    void upsertUpdatesTheRowWithTheSameKey() throws SQLException {
        upsert("a", 1, "first");
        upsert("a", 1, "second");
        upsert("a", 2, "other");

        assertEquals(Optional.of("second"), value("a", 1));
        assertEquals(Optional.of("other"), value("a", 2));
        assertEquals(2, count());
    }

    @Test
    void insertIfAbsentLeavesAnExistingRowAlone() throws SQLException {
        upsert("a", 1, "kept");

        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     database.dialect().insertIfAbsent(table, List.of("k", "n", "v")))) {
            for (String[] row : new String[][]{{"a", "kept-not"}, {"b", "added"}}) {
                insert.setString(1, row[0]);
                insert.setInt(2, 1);
                insert.setString(3, row[1]);
                insert.executeUpdate();
            }
        }

        assertEquals(Optional.of("kept"), value("a", 1));
        assertEquals(Optional.of("added"), value("b", 1));
    }

    @Test
    void aLargeBlobColumnHoldsAMegabyte() throws SQLException {
        byte[] bytes = new byte[1024 * 1024];
        Arrays.fill(bytes, (byte) 7);
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO " + table + " (k, n, v, b) VALUES ('big', 1, 'x', ?)")) {
            insert.setBytes(1, bytes);
            insert.executeUpdate();
        }

        try (Connection connection = database.dataSource().getConnection();
             Statement select = connection.createStatement();
             ResultSet result = select.executeQuery("SELECT b FROM " + table + " WHERE k = 'big'")) {
            result.next();
            assertArrayEquals(bytes, result.getBytes(1));
        }
    }

    private void upsert(String key, int number, String value) throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement upsert = connection.prepareStatement(
                     database.dialect().upsert(table, List.of("k", "n"), List.of("v")))) {
            upsert.setString(1, key);
            upsert.setInt(2, number);
            upsert.setString(3, value);
            upsert.executeUpdate();
        }
    }

    private Optional<String> value(String key, int number) throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement("SELECT v FROM " + table + " WHERE k = ? AND n = ?")) {
            select.setString(1, key);
            select.setInt(2, number);
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? Optional.of(result.getString(1)) : Optional.empty();
            }
        }
    }

    private int count() throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             Statement select = connection.createStatement();
             ResultSet result = select.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
