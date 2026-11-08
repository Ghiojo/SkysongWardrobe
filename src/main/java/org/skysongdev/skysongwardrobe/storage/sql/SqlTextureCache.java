package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link TextureCache} in the {@code textures} table: one row per signed skin, so storing a
 * skin writes one row and a lookup reads one, however many thousands are kept. Skins are never
 * deleted, so one is never signed twice. Every call goes to the database, so call it off the
 * main thread; a database error is thrown as {@link StorageException}.
 */
public final class SqlTextureCache implements TextureCache {
    private static final List<String> KEY = List.of("hash");
    private static final List<String> COLUMNS = List.of("texture_value", "texture_signature", "value_hash", "created_at");

    private final SqlDatabase database;
    private final Clock clock;
    private final String table;

    public SqlTextureCache(SqlDatabase database, Clock clock) {
        this.database = database;
        this.clock = clock;
        this.table = database.table("textures");
    }

    /**
     * Schema step 1. {@code value_hash} is the SHA-256 of the texture value: values are too long
     * to index in MySQL, so {@link #isOwn} looks up their hash instead. The columns aren't named
     * plain {@code value} and {@code signature}, which some databases reserve.
     */
    static List<String> createTables(SqlDatabase database) {
        String table = database.table("textures");
        return List.of(
                "CREATE TABLE " + table + " (hash VARCHAR(64) NOT NULL PRIMARY KEY, texture_value TEXT NOT NULL,"
                        + " texture_signature TEXT NOT NULL, value_hash VARCHAR(64) NOT NULL, created_at BIGINT NOT NULL)",
                "CREATE INDEX " + table + "_value_hash ON " + table + " (value_hash)");
    }

    @Override
    public Optional<SignedTexture> find(String hash) {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT texture_value, texture_signature FROM " + table + " WHERE hash = ?")) {
            select.setString(1, hash);
            try (ResultSet result = select.executeQuery()) {
                return result.next()
                        ? Optional.of(new SignedTexture(result.getString(1), result.getString(2)))
                        : Optional.empty();
            }
        } catch (SQLException failed) {
            throw new StorageException("Could not look up a signed skin", failed);
        }
    }

    @Override
    public boolean isOwn(SignedTexture texture) {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT texture_value FROM " + table + " WHERE value_hash = ?")) {
            select.setString(1, valueHash(texture.value()));
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    if (result.getString(1).equals(texture.value())) {
                        return true;
                    }
                }
                return false;
            }
        } catch (SQLException failed) {
            throw new StorageException("Could not check whether a skin is ours", failed);
        }
    }

    @Override
    public void put(String hash, SignedTexture texture) throws IOException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement upsert = connection.prepareStatement(database.dialect().upsert(table, KEY, COLUMNS))) {
            upsert.setString(1, hash);
            upsert.setString(2, texture.value());
            upsert.setString(3, texture.signature());
            upsert.setString(4, valueHash(texture.value()));
            upsert.setLong(5, clock.millis());
            upsert.executeUpdate();
        } catch (SQLException failed) {
            throw new IOException("Could not store a signed skin", failed);
        }
    }

    /**
     * One batch in one transaction, so thousands of imported skins take seconds, not minutes.
     * The rows are counted before and after, since drivers may not report each insert's count.
     */
    @Override
    public int putAllIfAbsent(Map<String, SignedTexture> textures) throws IOException {
        List<String> columns = SqlDialect.concat(KEY, COLUMNS);
        try (Connection connection = database.dataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(database.dialect().insertIfAbsent(table, columns))) {
                long before = count(connection);
                long now = clock.millis();
                for (Map.Entry<String, SignedTexture> entry : textures.entrySet()) {
                    insert.setString(1, entry.getKey());
                    insert.setString(2, entry.getValue().value());
                    insert.setString(3, entry.getValue().signature());
                    insert.setString(4, valueHash(entry.getValue().value()));
                    insert.setLong(5, now);
                    insert.addBatch();
                }
                insert.executeBatch();
                int added = (int) (count(connection) - before);
                connection.commit();
                return added;
            } catch (SQLException failed) {
                connection.rollback();
                throw failed;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException failed) {
            throw new IOException("Could not import the signed skins", failed);
        }
    }

    private long count(Connection connection) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet result = select.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    static String valueHash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java runtime has SHA-256", impossible);
        }
    }
}
