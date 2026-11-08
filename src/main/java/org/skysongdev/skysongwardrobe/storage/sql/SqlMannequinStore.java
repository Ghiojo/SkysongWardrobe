package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.mannequin.MannequinStore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Mannequin records in the {@code mannequins} table, one row each. */
public final class SqlMannequinStore implements MannequinStore {
    private static final List<String> KEY = List.of("id");
    private static final List<String> COLUMNS = List.of("owner", "skin", "model", "spawner");

    private final SqlDatabase database;
    private final String table;

    public SqlMannequinStore(SqlDatabase database) {
        this.database = database;
        this.table = database.table("mannequins");
    }

    /** Schema step 3. */
    static List<String> createTables(SqlDatabase database) {
        String table = database.table("mannequins");
        return List.of(
                "CREATE TABLE " + table + " (id VARCHAR(36) NOT NULL PRIMARY KEY, owner VARCHAR(36) NOT NULL,"
                        + " skin VARCHAR(64) NOT NULL, model VARCHAR(16) NOT NULL, spawner "
                        + database.dialect().largeBlobType() + " NOT NULL)",
                "CREATE INDEX " + table + "_owner ON " + table + " (owner)");
    }

    @Override
    public Contents readAll() throws IOException {
        List<Row> rows = new ArrayList<>();
        Set<UUID> unreadable = new HashSet<>();
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT id, owner, skin, model, spawner FROM " + table);
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                UUID id = UUID.fromString(result.getString(1));
                try {
                    rows.add(new Row(id, UUID.fromString(result.getString(2)), result.getString(3), result.getString(4),
                            result.getBytes(5)));
                } catch (IllegalArgumentException broken) {
                    unreadable.add(id);
                }
            }
            return new Contents(rows, unreadable);
        } catch (SQLException failed) {
            throw new IOException("Could not read the mannequins", failed);
        }
    }

    @Override
    public void write(Row row) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement upsert = connection.prepareStatement(database.dialect().upsert(table, KEY, COLUMNS))) {
            upsert.setString(1, row.id().toString());
            upsert.setString(2, row.owner().toString());
            upsert.setString(3, row.skin());
            upsert.setString(4, row.model());
            upsert.setBytes(5, row.spawner());
            upsert.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not save mannequin " + row.id(), failed);
        }
    }

    @Override
    public void delete(UUID id) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            delete.setString(1, id.toString());
            delete.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not delete mannequin " + id, failed);
        }
    }
}
