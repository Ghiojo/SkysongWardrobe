package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * One kind of wardrobe in the {@code wardrobe_items} table, one row per filled slot. Every kind
 * shares the table and is told apart by {@code wardrobe_kind}, such as {@code player}. A write
 * replaces the owner's rows in one transaction, so a crash never leaves half an outfit.
 */
public final class SqlWardrobeStore implements WardrobeStore {
    /** Wardrobes kept aside take this suffix on their kind, so they are never read but never lost. */
    static final String KEPT_ASIDE = "_broken";

    private final SqlDatabase database;
    private final String table;
    private final String kind;

    public SqlWardrobeStore(SqlDatabase database, WardrobeKind kind) {
        this.database = database;
        this.table = database.table("wardrobe_items");
        this.kind = kind.name().toLowerCase(Locale.ROOT);
    }

    /** Schema step 2. */
    static List<String> createTables(SqlDatabase database) {
        return List.of("CREATE TABLE " + database.table("wardrobe_items") + " (wardrobe_kind VARCHAR(32) NOT NULL,"
                + " owner VARCHAR(36) NOT NULL, slot VARCHAR(64) NOT NULL, item " + database.dialect().largeBlobType()
                + " NOT NULL, PRIMARY KEY (wardrobe_kind, owner, slot))");
    }

    @Override
    public Map<String, byte[]> read(UUID owner) throws IOException {
        Map<String, byte[]> slots = new LinkedHashMap<>();
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT slot, item FROM " + table + " WHERE wardrobe_kind = ? AND owner = ? ORDER BY slot")) {
            select.setString(1, kind);
            select.setString(2, owner.toString());
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    slots.put(result.getString(1), result.getBytes(2));
                }
            }
            return slots;
        } catch (SQLException failed) {
            throw new IOException("Could not read the " + kind + " wardrobe of " + owner, failed);
        }
    }

    @Override
    public void write(UUID owner, Map<String, byte[]> slots) throws IOException {
        inTransaction(owner, "save", connection -> {
            deleteRows(connection, kind, owner);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + table + " (wardrobe_kind, owner, slot, item) VALUES (?, ?, ?, ?)")) {
                for (Map.Entry<String, byte[]> slot : slots.entrySet()) {
                    insert.setString(1, kind);
                    insert.setString(2, owner.toString());
                    insert.setString(3, slot.getKey());
                    insert.setBytes(4, slot.getValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        });
    }

    /** Keeping aside moves the rows to {@code <kind>_broken}, replacing any kept there before. */
    @Override
    public void delete(UUID owner, boolean keepAside) throws IOException {
        inTransaction(owner, "delete", connection -> {
            if (keepAside) {
                deleteRows(connection, kind + KEPT_ASIDE, owner);
                try (PreparedStatement move = connection.prepareStatement(
                        "UPDATE " + table + " SET wardrobe_kind = ? WHERE wardrobe_kind = ? AND owner = ?")) {
                    move.setString(1, kind + KEPT_ASIDE);
                    move.setString(2, kind);
                    move.setString(3, owner.toString());
                    move.executeUpdate();
                }
            } else {
                deleteRows(connection, kind, owner);
            }
        });
    }

    private void deleteRows(Connection connection, String rowKind, UUID owner) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE wardrobe_kind = ? AND owner = ?")) {
            delete.setString(1, rowKind);
            delete.setString(2, owner.toString());
            delete.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface Work {
        void run(Connection connection) throws SQLException;
    }

    private void inTransaction(UUID owner, String action, Work work) throws IOException {
        try (Connection connection = database.dataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException failed) {
                connection.rollback();
                throw failed;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException failed) {
            throw new IOException("Could not " + action + " the " + kind + " wardrobe of " + owner, failed);
        }
    }
}
