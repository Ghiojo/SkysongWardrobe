package org.skysongdev.skysongwardrobe.storage;

import java.util.Objects;

/**
 * The MySQL connection from {@code storage.mysql} in config.yml. The keys match SkysongStats'
 * {@code database:} section, so the same values can be copied across, but SkysongWardrobe keeps
 * its own copy and never needs SkysongStats installed.
 *
 * @param tablePrefix put before every table name, such as {@code sw_wardrobe_items}, so these
 *                    tables never clash with another plugin's in the same database
 * @param poolSize    the most connections open at once
 */
public record DatabaseSettings(String host, int port, String database, String user, String password,
                               String tablePrefix, int poolSize) {

    public DatabaseSettings {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(tablePrefix, "tablePrefix");
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    /** Keeps the password out of logs and error messages. */
    @Override
    public String toString() {
        return "DatabaseSettings[" + user + "@" + host + ":" + port + "/" + database
                + ", prefix " + tablePrefix + ", pool " + poolSize + "]";
    }
}
