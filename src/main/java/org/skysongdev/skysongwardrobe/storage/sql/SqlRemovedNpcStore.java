package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.npc.RemovedNpc;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Removed-NPC entries in the {@code removed_npcs} table. {@code removed_at} is epoch milliseconds,
 * which is as precise as the archive ever needs.
 */
public final class SqlRemovedNpcStore implements RemovedNpcStore {
    private static final List<String> KEY = List.of("entry");
    private static final List<String> COLUMNS = List.of("npc", "number", "name", "removed_at");

    private final SqlDatabase database;
    private final String table;

    public SqlRemovedNpcStore(SqlDatabase database) {
        this.database = database;
        this.table = database.table("removed_npcs");
    }

    /** Schema step 5. */
    static List<String> createTables(SqlDatabase database) {
        return List.of("CREATE TABLE " + database.table("removed_npcs") + " (entry VARCHAR(36) NOT NULL PRIMARY KEY,"
                + " npc VARCHAR(36) NOT NULL, number INT NOT NULL, name VARCHAR(255) NOT NULL,"
                + " removed_at BIGINT NOT NULL)");
    }

    @Override
    public List<RemovedNpc> readAll() throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT entry, npc, number, name, removed_at FROM " + table);
             ResultSet result = select.executeQuery()) {
            List<RemovedNpc> entries = new ArrayList<>();
            while (result.next()) {
                entries.add(new RemovedNpc(UUID.fromString(result.getString(1)), UUID.fromString(result.getString(2)),
                        result.getInt(3), result.getString(4), Instant.ofEpochMilli(result.getLong(5))));
            }
            return entries;
        } catch (SQLException failed) {
            throw new IOException("Could not read the removed NPCs", failed);
        }
    }

    @Override
    public void write(RemovedNpc entry) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement upsert = connection.prepareStatement(database.dialect().upsert(table, KEY, COLUMNS))) {
            upsert.setString(1, entry.entry().toString());
            upsert.setString(2, entry.npc().toString());
            upsert.setInt(3, entry.number());
            upsert.setString(4, entry.name());
            upsert.setLong(5, entry.removedAt().toEpochMilli());
            upsert.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not save removed NPC entry " + entry.entry(), failed);
        }
    }

    @Override
    public void delete(UUID entry) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE entry = ?")) {
            delete.setString(1, entry.toString());
            delete.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not delete removed NPC entry " + entry, failed);
        }
    }
}
