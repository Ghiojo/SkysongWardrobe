package org.skysongdev.skysongwardrobe.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.skysongdev.skysongwardrobe.storage.DatabaseSettings;
import org.skysongdev.skysongwardrobe.storage.StorageSettings;
import org.skysongdev.skysongwardrobe.storage.StorageType;

import java.nio.file.Path;

/**
 * Builds the HikariCP pool for {@code storage.type} sqlite or mysql. The pool checks connections
 * before handing them out and replaces ones the database dropped, so a database restart doesn't
 * break the plugin. Opening fails at once when no first connection can be made, so a wrong
 * password stops the plugin at start rather than on the first save.
 */
public final class ConnectionPool {
    static final String POOL_NAME = "SkysongWardrobe";
    /** SQLite lets one connection write at a time; one pooled connection avoids "database is locked". */
    static final int SQLITE_POOL_SIZE = 1;
    static final String SQLITE_TABLE_PREFIX = "sw_";

    private ConnectionPool() {
    }

    /** Opens the database the settings name; {@code dataFolder} holds the SQLite file. */
    public static Opened open(StorageSettings settings, Path dataFolder) {
        HikariDataSource pool = new HikariDataSource(config(settings, dataFolder));
        return switch (settings.type()) {
            case MYSQL -> new Opened(pool, new SqlDatabase(pool, new MySqlDialect(), settings.mysql().tablePrefix()));
            case SQLITE -> new Opened(pool, new SqlDatabase(pool, new SqliteDialect(), SQLITE_TABLE_PREFIX));
            case YAML -> {
                pool.close();
                throw new IllegalArgumentException("storage.type yaml has no database to open");
            }
        };
    }

    static HikariConfig config(StorageSettings settings, Path dataFolder) {
        if (settings.type() == StorageType.YAML) {
            throw new IllegalArgumentException("storage.type yaml has no database to open");
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName(POOL_NAME);
        config.setInitializationFailTimeout(1);
        if (settings.type() == StorageType.MYSQL) {
            DatabaseSettings mysql = settings.mysql();
            config.setJdbcUrl(mysql.jdbcUrl());
            config.setUsername(mysql.user());
            config.setPassword(mysql.password());
            config.setMaximumPoolSize(mysql.poolSize());
        } else {
            config.setJdbcUrl("jdbc:sqlite:" + dataFolder.resolve(settings.sqliteFile()).toAbsolutePath());
            config.setMaximumPoolSize(SQLITE_POOL_SIZE);
            // Waits for a lock instead of failing at once, should anything else open the file.
            config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
        }
        return config;
    }

    /**
     * An open pool and the database over it. Close it when the plugin disables, after the last
     * write finished.
     */
    public record Opened(HikariDataSource pool, SqlDatabase database) implements AutoCloseable {
        @Override
        public void close() {
            pool.close();
        }
    }
}
