package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStoreContract;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SqlWardrobeStore} on one kind of database; each database's test fills in {@link #database()}. */
abstract class SqlWardrobeStoreContract extends WardrobeStoreContract {
    private static final UUID OWNER = UUID.randomUUID();

    private SqlDatabase database;

    abstract SqlDatabase database();

    @Override
    protected WardrobeStore emptyStore() {
        database = database();
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        return new SqlWardrobeStore(database, WardrobeKind.PLAYER);
    }

    private int rows(String kind) throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sw_wardrobe_items WHERE wardrobe_kind = ?")) {
            select.setString(1, kind);
            try (ResultSet result = select.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    @Test
    public void kindsWithTheSameOwnerAreKeptApart() throws IOException {
        SqlWardrobeStore players = new SqlWardrobeStore(database, WardrobeKind.PLAYER);
        SqlWardrobeStore mannequins = new SqlWardrobeStore(database, WardrobeKind.MANNEQUIN);
        players.write(OWNER, Map.of("coat", new byte[]{1}));
        mannequins.write(OWNER, Map.of("coat", new byte[]{2}));

        mannequins.delete(OWNER, false);

        assertArrayEquals(new byte[]{1}, players.read(OWNER).get("coat"));
        assertTrue(mannequins.read(OWNER).isEmpty());
    }

    @Test
    public void keepingAsideMovesTheRowsToTheBrokenKind() throws IOException, SQLException {
        SqlWardrobeStore players = new SqlWardrobeStore(database, WardrobeKind.PLAYER);
        players.write(OWNER, Map.of("coat", new byte[]{1}, "boots", new byte[]{2}));

        players.delete(OWNER, true);
        players.write(OWNER, Map.of("coat", new byte[]{3}));
        players.delete(OWNER, true);

        assertEquals(0, rows("player"));
        assertEquals(1, rows("player_broken"));
    }

    @Test
    public void aMegabyteItemFits() throws IOException {
        byte[] big = new byte[1024 * 1024];
        big[big.length - 1] = 9;
        SqlWardrobeStore players = new SqlWardrobeStore(database, WardrobeKind.PLAYER);

        players.write(OWNER, Map.of("chest", big));

        assertArrayEquals(big, players.read(OWNER).get("chest"));
    }
}
