package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.TextureCacheContract;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SqlTextureCache} on one kind of database; each database's test fills in {@link #database()}. */
abstract class SqlTextureCacheContract extends TextureCacheContract {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

    private SqlDatabase database;

    /** A new, empty database. */
    abstract SqlDatabase database();

    @Override
    protected TextureCache emptyCache() {
        database = database();
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        return new SqlTextureCache(database, CLOCK);
    }

    @Test
    public void storedTexturesSurviveANewCacheOnTheSameDatabase() throws IOException {
        SignedTexture texture = new SignedTexture("value", "signature");
        new SqlTextureCache(database, CLOCK).put("abc", texture);

        SqlTextureCache reopened = new SqlTextureCache(database, CLOCK);

        assertEquals(Optional.of(texture), reopened.find("abc"));
        assertTrue(reopened.isOwn(texture));
    }

    @Test
    public void storesWhenTheSkinWasSignedAndTheHashOfItsValue() throws IOException, SQLException {
        new SqlTextureCache(database, CLOCK).put("abc", new SignedTexture("value", "signature"));

        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT value_hash, created_at FROM sw_textures WHERE hash = 'abc'");
             ResultSet result = select.executeQuery()) {
            assertTrue(result.next());
            assertEquals(SqlTextureCache.valueHash("value"), result.getString(1));
            assertEquals(CLOCK.millis(), result.getLong(2));
        }
    }

    @Test
    public void aMissingTableIsAStorageErrorNotAMiss() {
        SqlTextureCache unmigrated = new SqlTextureCache(new SqlDatabase(database.dataSource(), database.dialect(),
                "other_"), CLOCK);

        assertThrows(StorageException.class, () -> unmigrated.find("abc"));
        assertThrows(StorageException.class, () -> unmigrated.isOwn(new SignedTexture("v", "s")));
        assertThrows(IOException.class, () -> unmigrated.put("abc", new SignedTexture("v", "s")));
    }
}
