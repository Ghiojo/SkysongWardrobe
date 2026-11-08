package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.npc.NpcRecord;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStore;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NPC records in the {@code npcs} table: each dressed NPC's base skin. */
public final class SqlNpcRecordStore implements NpcRecordStore {
    private static final List<String> KEY = List.of("id");
    private static final List<String> COLUMNS = List.of("base_value", "base_signature");

    private final SqlDatabase database;
    private final String table;

    public SqlNpcRecordStore(SqlDatabase database) {
        this.database = database;
        this.table = database.table("npcs");
    }

    /** Schema step 4. */
    static List<String> createTables(SqlDatabase database) {
        return List.of("CREATE TABLE " + database.table("npcs") + " (id VARCHAR(36) NOT NULL PRIMARY KEY,"
                + " base_value TEXT NOT NULL, base_signature TEXT NOT NULL)");
    }

    @Override
    public Optional<NpcRecord> read(UUID id) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT base_value, base_signature FROM " + table + " WHERE id = ?")) {
            select.setString(1, id.toString());
            try (ResultSet result = select.executeQuery()) {
                return result.next()
                        ? Optional.of(new NpcRecord(id, new SignedTexture(result.getString(1), result.getString(2))))
                        : Optional.empty();
            }
        } catch (SQLException failed) {
            throw new IOException("Could not read the record of NPC " + id, failed);
        }
    }

    @Override
    public void write(NpcRecord record) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement upsert = connection.prepareStatement(database.dialect().upsert(table, KEY, COLUMNS))) {
            upsert.setString(1, record.id().toString());
            upsert.setString(2, record.base().value());
            upsert.setString(3, record.base().signature());
            upsert.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not save the record of NPC " + record.id(), failed);
        }
    }

    @Override
    public void delete(UUID id) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            delete.setString(1, id.toString());
            delete.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not delete the record of NPC " + id, failed);
        }
    }
}
