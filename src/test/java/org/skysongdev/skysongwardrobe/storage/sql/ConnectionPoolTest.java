package org.skysongdev.skysongwardrobe.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.storage.DatabaseSettings;
import org.skysongdev.skysongwardrobe.storage.StorageSettings;
import org.skysongdev.skysongwardrobe.storage.StorageType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionPoolTest {
    private static final DatabaseSettings MYSQL =
            new DatabaseSettings("db.example", 3307, "skysong", "wardrobe", "hunter2", "wardrobe_", 6);

    @TempDir
    Path folder;

    @Test
    void mysqlUsesTheConfiguredConnectionAndPoolSize() {
        HikariConfig config = ConnectionPool.config(new StorageSettings(StorageType.MYSQL, "data.db", MYSQL), folder);

        assertEquals("jdbc:mysql://db.example:3307/skysong", config.getJdbcUrl());
        assertEquals("wardrobe", config.getUsername());
        assertEquals("hunter2", config.getPassword());
        assertEquals(6, config.getMaximumPoolSize());
        assertEquals("SkysongWardrobe", config.getPoolName());
        assertEquals(1, config.getInitializationFailTimeout());
    }

    @Test
    void sqliteUsesOneConnectionToAFileInThePluginFolder() {
        HikariConfig config = ConnectionPool.config(new StorageSettings(StorageType.SQLITE, "data.db", MYSQL), folder);

        assertEquals("jdbc:sqlite:" + folder.resolve("data.db").toAbsolutePath(), config.getJdbcUrl());
        assertEquals(1, config.getMaximumPoolSize());
    }

    @Test
    void opensAWorkingSqliteDatabase() throws Exception {
        try (ConnectionPool.Opened opened = ConnectionPool.open(
                new StorageSettings(StorageType.SQLITE, "data.db", MYSQL), folder)) {
            assertInstanceOf(SqliteDialect.class, opened.database().dialect());
            assertEquals("sw_textures", opened.database().table("textures"));
            try (Connection connection = opened.database().dataSource().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT 1")) {
                assertTrue(result.next());
            }
        }
        assertTrue(Files.isRegularFile(folder.resolve("data.db")));
    }

    @Test
    void yamlHasNoDatabaseToOpen() {
        assertThrows(IllegalArgumentException.class,
                () -> ConnectionPool.config(new StorageSettings(StorageType.YAML, "data.db", MYSQL), folder));
    }
}
