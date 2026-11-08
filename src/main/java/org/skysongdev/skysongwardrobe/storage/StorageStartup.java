package org.skysongdev.skysongwardrobe.storage;

import org.skysongdev.skysongwardrobe.storage.sql.ConnectionPool;
import org.skysongdev.skysongwardrobe.storage.sql.SqlStorage;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens the storage {@code storage:} in config.yml asks for. When that can't be done (a wrong
 * setting, a database that won't answer, tables that can't be made) it logs one clear line and
 * gives nothing back, and the plugin disables itself: falling back to files would split the
 * data between two places.
 */
public final class StorageStartup {
    private StorageStartup() {
    }

    /** Reads the {@code storage:} settings; may refuse them. */
    @FunctionalInterface
    public interface SettingsSource {
        StorageSettings read() throws InvalidStorageSettingsException;
    }

    public static Optional<StorageBackend> open(SettingsSource settings, Path dataFolder, Clock clock, Logger logger) {
        StorageSettings read;
        try {
            read = settings.read();
        } catch (InvalidStorageSettingsException invalid) {
            logger.severe("config.yml: " + invalid.getMessage() + " SkysongWardrobe stays disabled until it is fixed.");
            return Optional.empty();
        }
        try {
            StorageBackend backend = switch (read.type()) {
                case YAML -> new YamlStorage(dataFolder, logger);
                case SQLITE, MYSQL -> sql(read, dataFolder, clock, logger);
            };
            logger.info("Storing data in " + describe(read) + ".");
            return Optional.of(backend);
        } catch (RuntimeException | LinkageError failed) {
            logger.log(Level.SEVERE, "Could not open " + describe(read) + ", so SkysongWardrobe stays disabled. Check"
                    + " the storage: section of config.yml and that the database is running.", failed);
            return Optional.empty();
        }
    }

    private static StorageBackend sql(StorageSettings settings, Path dataFolder, Clock clock, Logger logger) {
        ConnectionPool.Opened opened = ConnectionPool.open(settings, dataFolder);
        try {
            return new SqlStorage(opened.database(), opened, clock, logger);
        } catch (RuntimeException failed) {
            opened.close();
            throw failed;
        }
    }

    private static String describe(StorageSettings settings) {
        return switch (settings.type()) {
            case YAML -> "YAML files";
            case SQLITE -> "the SQLite database " + settings.sqliteFile();
            case MYSQL -> "the MySQL database " + settings.mysql();
        };
    }
}
