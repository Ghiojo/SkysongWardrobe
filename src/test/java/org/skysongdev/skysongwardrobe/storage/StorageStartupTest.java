package org.skysongdev.skysongwardrobe.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;
import org.skysongdev.skysongwardrobe.storage.sql.SqlTextureCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageStartupTest {
    private static final SignedTexture SIGNED = new SignedTexture("value", "signature");
    private static final DatabaseSettings MYSQL =
            new DatabaseSettings("127.0.0.1", 1, "skysong", "root", "", "sw_", 1);

    @TempDir
    Path folder;

    private final Logger logger = Logger.getAnonymousLogger();
    private final List<LogRecord> severe = new ArrayList<>();

    @BeforeEach
    void captureLogs() {
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {
                    severe.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    private Optional<StorageBackend> open(StorageType type) {
        return StorageStartup.open(() -> new StorageSettings(type, "data.db", MYSQL), folder, Clock.systemUTC(), logger);
    }

    @Test
    void yamlKeepsSignedSkinsInTextureCacheYml() throws IOException {
        StorageBackend storage = open(StorageType.YAML).orElseThrow();

        assertInstanceOf(YamlTextureCache.class, storage.textures());
        storage.textures().put("abc", SIGNED);
        assertTrue(Files.isRegularFile(folder.resolve("texture-cache.yml")));
        storage.close();
    }

    @Test
    void sqliteKeepsSignedSkinsInTheDatabaseAcrossRestarts() throws IOException {
        try (StorageBackend storage = open(StorageType.SQLITE).orElseThrow()) {
            assertInstanceOf(SqlTextureCache.class, storage.textures());
            storage.textures().put("abc", SIGNED);
        }

        try (StorageBackend restarted = open(StorageType.SQLITE).orElseThrow()) {
            assertEquals(Optional.of(SIGNED), restarted.textures().find("abc"));
        }
        assertTrue(Files.isRegularFile(folder.resolve("data.db")));
        assertTrue(Files.notExists(folder.resolve("texture-cache.yml")));
    }

    @Test
    void invalidSettingsLogOneLineAndOpenNothing() {
        Optional<StorageBackend> storage = StorageStartup.open(() -> {
            throw new InvalidStorageSettingsException("storage.type is 'mysq'; it must be yaml, sqlite or mysql.");
        }, folder, Clock.systemUTC(), logger);

        assertEquals(Optional.empty(), storage);
        assertEquals(1, severe.size());
        assertTrue(severe.getFirst().getMessage().contains("mysq"));
    }

    @Test
    void aDatabaseThatCantBeReachedOpensNothing() {
        assertEquals(Optional.empty(), open(StorageType.MYSQL));
        assertEquals(1, severe.size());
        assertTrue(severe.getFirst().getMessage().contains("stays disabled"));
    }
}
