package org.skysongdev.skysongwardrobe.storage;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageSettingsParserTest {
    private final StorageSettingsParser parser = new StorageSettingsParser();

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    @Test
    void noStorageSectionMeansYamlWithDefaults() throws InvalidStorageSettingsException {
        StorageSettings settings = parser.parse(new YamlConfiguration());

        assertEquals(StorageType.YAML, settings.type());
        assertEquals("data.db", settings.sqliteFile());
        assertEquals(new DatabaseSettings("localhost", 3306, "skysong", "root", "", "sw_", 4), settings.mysql());
        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void theBundledConfigIsValidAndUsesYaml() throws InvalidStorageSettingsException {
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")), StandardCharsets.UTF_8));

        StorageSettings settings = parser.parse(bundled);

        assertEquals(StorageType.YAML, settings.type());
        assertEquals(new DatabaseSettings("localhost", 3306, "skysong", "root", "", "sw_", 4), settings.mysql());
        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void readsEveryMySqlValue() throws Exception {
        StorageSettings settings = parser.parse(yaml("""
                storage:
                  type: MySQL
                  mysql:
                    host: " db.example "
                    port: 3307
                    user: wardrobe
                    password: hunter2
                    database_name: skysong_wardrobe
                    table-prefix: wardrobe_
                    pool-size: 8
                """));

        assertEquals(StorageType.MYSQL, settings.type());
        assertEquals(new DatabaseSettings("db.example", 3307, "skysong_wardrobe", "wardrobe", "hunter2", "wardrobe_", 8),
                settings.mysql());
        assertEquals("jdbc:mysql://db.example:3307/skysong_wardrobe", settings.mysql().jdbcUrl());
    }

    @Test
    void readsTheSqliteFile() throws Exception {
        StorageSettings settings = parser.parse(yaml("storage:\n  type: sqlite\n  sqlite:\n    file: wardrobe.db\n"));

        assertEquals(StorageType.SQLITE, settings.type());
        assertEquals("wardrobe.db", settings.sqliteFile());
    }

    @Test
    void anUnknownTypeStopsThePlugin() throws InvalidConfigurationException {
        InvalidStorageSettingsException thrown = assertThrows(InvalidStorageSettingsException.class,
                () -> parser.parse(yaml("storage:\n  type: mysq\n")));

        assertTrue(thrown.getMessage().contains("mysq"));
    }

    @Test
    void anUnsafeTablePrefixStopsThePlugin() throws InvalidConfigurationException {
        assertThrows(InvalidStorageSettingsException.class, () -> parser.parse(yaml("""
                storage:
                  type: mysql
                  mysql:
                    table-prefix: "sw; DROP TABLE x; --"
                """)));
    }

    @Test
    void aBadDatabaseNameOrPortStopsThePlugin() throws InvalidConfigurationException {
        assertThrows(InvalidStorageSettingsException.class,
                () -> parser.parse(yaml("storage:\n  type: mysql\n  mysql:\n    database_name: \"\"\n")));
        assertThrows(InvalidStorageSettingsException.class,
                () -> parser.parse(yaml("storage:\n  type: mysql\n  mysql:\n    port: \"abc\"\n")));
        assertThrows(InvalidStorageSettingsException.class,
                () -> parser.parse(yaml("storage:\n  type: mysql\n  mysql:\n    port: 70000\n")));
    }

    @Test
    void anEmptySqliteFileStopsThePlugin() throws InvalidConfigurationException {
        assertThrows(InvalidStorageSettingsException.class,
                () -> parser.parse(yaml("storage:\n  type: sqlite\n  sqlite:\n    file: \" \"\n")));
    }

    @Test
    void aBrokenMySqlSectionDoesNotStopYaml() throws Exception {
        StorageSettings settings = parser.parse(yaml("""
                storage:
                  type: yaml
                  mysql:
                    table-prefix: "not valid!"
                    port: nope
                """));

        assertEquals(StorageType.YAML, settings.type());
    }

    @Test
    void aBadPoolSizeFallsBackWithAWarning() throws Exception {
        StorageSettings settings = parser.parse(yaml("storage:\n  type: mysql\n  mysql:\n    pool-size: 0\n"));

        assertEquals(4, settings.mysql().poolSize());
        assertEquals(List.of("storage.mysql.pool-size must be a whole number from 1 to 32, so it uses 4."),
                parser.warnings());
    }

    @Test
    void thePasswordNeverReachesToString() {
        DatabaseSettings settings = new DatabaseSettings("localhost", 3306, "skysong", "root", "hunter2", "sw_", 4);

        assertFalse(settings.toString().contains("hunter2"));
        assertTrue(settings.toString().contains("root@localhost:3306/skysong"));
    }
}
