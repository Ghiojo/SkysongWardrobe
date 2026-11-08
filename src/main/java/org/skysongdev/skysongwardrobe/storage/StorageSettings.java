package org.skysongdev.skysongwardrobe.storage;

import java.util.Objects;

/**
 * The {@code storage:} section of config.yml, already checked. Built by {@link StorageSettingsParser}.
 * It is read once at start: changing it takes a restart, never {@code /sw reload}.
 *
 * @param type       where the data is kept
 * @param sqliteFile the SQLite database file, relative to the plugin folder
 * @param mysql      the MySQL connection, used only when {@code type} is {@link StorageType#MYSQL}
 */
public record StorageSettings(StorageType type, String sqliteFile, DatabaseSettings mysql) {

    public StorageSettings {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sqliteFile, "sqliteFile");
        Objects.requireNonNull(mysql, "mysql");
    }
}
