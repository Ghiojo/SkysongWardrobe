package org.skysongdev.skysongwardrobe.storage;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns the {@code storage:} section of config.yml into {@link StorageSettings}. Unlike the rest
 * of config.yml, a wrong type, prefix or database name stops the plugin: a guessed default would
 * send data to the wrong place. A wrong pool size only falls back to its default, with a warning.
 */
public final class StorageSettingsParser {
    static final String DEFAULT_SQLITE_FILE = "data.db";
    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 3306;
    static final String DEFAULT_DATABASE = "skysong";
    static final String DEFAULT_USER = "root";
    static final String DEFAULT_TABLE_PREFIX = "sw_";
    static final int DEFAULT_POOL_SIZE = 4;
    static final int MAX_POOL_SIZE = 32;
    /** The prefix is written into SQL, so it may only hold characters a table name can. */
    private static final Pattern TABLE_PREFIX = Pattern.compile("[A-Za-z0-9_]{0,32}");
    private static final Pattern DATABASE_NAME = Pattern.compile("[A-Za-z0-9_$-]{1,64}");

    private final List<String> warnings = new ArrayList<>();

    /** Reads {@code storage:} from the whole config; {@link #warnings()} then lists any fallbacks. */
    public StorageSettings parse(ConfigurationSection config) throws InvalidStorageSettingsException {
        warnings.clear();
        StorageType type = type(config.getString("storage.type", "yaml"));
        String sqliteFile = config.getString("storage.sqlite.file", DEFAULT_SQLITE_FILE).trim();
        if (type == StorageType.SQLITE && sqliteFile.isEmpty()) {
            throw new InvalidStorageSettingsException("storage.sqlite.file is empty; name the database file, such as "
                    + DEFAULT_SQLITE_FILE + ".");
        }
        return new StorageSettings(type, sqliteFile, mysql(config, type == StorageType.MYSQL));
    }

    /** The values the last {@link #parse} replaced with their defaults, one line each. */
    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    private static StorageType type(String name) throws InvalidStorageSettingsException {
        return StorageType.named(name).orElseThrow(() -> new InvalidStorageSettingsException(
                "storage.type is '" + name + "'; it must be yaml, sqlite or mysql."));
    }

    /** Checks the connection strictly only when MySQL is used, so a stale section never blocks YAML. */
    private DatabaseSettings mysql(ConfigurationSection config, boolean used) throws InvalidStorageSettingsException {
        String database = config.getString("storage.mysql.database_name", DEFAULT_DATABASE).trim();
        String prefix = config.getString("storage.mysql.table-prefix", DEFAULT_TABLE_PREFIX).trim();
        int port = port(config.getString("storage.mysql.port", String.valueOf(DEFAULT_PORT)));
        if (used && !DATABASE_NAME.matcher(database).matches()) {
            throw new InvalidStorageSettingsException("storage.mysql.database_name '" + database
                    + "' must be 1 to 64 letters, digits, _, $ or -.");
        }
        if (used && !TABLE_PREFIX.matcher(prefix).matches()) {
            throw new InvalidStorageSettingsException("storage.mysql.table-prefix '" + prefix
                    + "' may only hold letters, digits and _, at most 32 of them.");
        }
        if (used && port < 1) {
            throw new InvalidStorageSettingsException("storage.mysql.port must be a number from 1 to 65535.");
        }
        return new DatabaseSettings(
                config.getString("storage.mysql.host", DEFAULT_HOST).trim(),
                port,
                database,
                config.getString("storage.mysql.user", DEFAULT_USER),
                config.getString("storage.mysql.password", ""),
                prefix,
                poolSize(config));
    }

    /** SkysongStats writes the port as a string, so both "3306" and 3306 are accepted; -1 when invalid. */
    private static int port(String raw) {
        try {
            int port = Integer.parseInt(raw.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    private int poolSize(ConfigurationSection config) {
        String path = "storage.mysql.pool-size";
        if (!config.contains(path)) {
            return DEFAULT_POOL_SIZE;
        }
        int size = config.getInt(path);
        if (!config.isInt(path) || size < 1 || size > MAX_POOL_SIZE) {
            warnings.add(path + " must be a whole number from 1 to " + MAX_POOL_SIZE + ", so it uses "
                    + DEFAULT_POOL_SIZE + ".");
            return DEFAULT_POOL_SIZE;
        }
        return size;
    }
}
