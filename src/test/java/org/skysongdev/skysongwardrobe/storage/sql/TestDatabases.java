package org.skysongdev.skysongwardrobe.storage.sql;

import org.h2.jdbcx.JdbcDataSource;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.UUID;

/** Real, throwaway databases for the SQL tests: SQLite in a file, and H2 standing in for MySQL. */
final class TestDatabases {
    private TestDatabases() {
    }

    /** A fresh SQLite database in {@code folder}. */
    static SqlDatabase sqlite(Path folder) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + folder.resolve(UUID.randomUUID() + ".db").toAbsolutePath());
        return new SqlDatabase(source, new SqliteDialect(), "sw_");
    }

    /** A fresh in-memory H2 database speaking MySQL, kept until the test JVM exits. */
    static SqlDatabase mysql() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return new SqlDatabase(source, new MySqlDialect(), "sw_");
    }
}
