package org.skysongdev.skysongwardrobe.storage.sql;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * An open database and how to talk to it: the SQL stores take one of these, never a URL or a
 * password, so they run the same on MySQL, SQLite and the test databases.
 *
 * @param tablePrefix put before every table name, already checked to be safe in SQL
 */
public record SqlDatabase(DataSource dataSource, SqlDialect dialect, String tablePrefix) {

    public SqlDatabase {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(dialect, "dialect");
        Objects.requireNonNull(tablePrefix, "tablePrefix");
    }

    /** The full name of one of this plugin's tables, such as {@code sw_textures} for {@code textures}. */
    public String table(String name) {
        return tablePrefix + name;
    }
}
