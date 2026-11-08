package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Brings the tables up to date. The {@code schema} table remembers the last step that ran, so
 * each {@link SchemaStep} runs exactly once per database, in order, and a newer plugin version
 * can add a column to a database an older one made.
 */
public final class SchemaMigrator {
    private final SqlDatabase database;
    private final Logger logger;

    public SchemaMigrator(SqlDatabase database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    /** Runs every step newer than the database's version. Blocks, so call it at start. */
    public void migrate(List<SchemaStep> steps) {
        String schema = database.table("schema");
        try (Connection connection = database.dataSource().getConnection()) {
            execute(connection, "CREATE TABLE IF NOT EXISTS " + schema + " (version INT NOT NULL)");
            int current = version(connection, schema);
            List<SchemaStep> pending = steps.stream()
                    .filter(step -> step.version() > current)
                    .sorted(Comparator.comparingInt(SchemaStep::version))
                    .toList();
            for (SchemaStep step : pending) {
                for (String sql : step.statements().apply(database)) {
                    execute(connection, sql);
                }
                setVersion(connection, schema, step.version());
                logger.info("Database schema step " + step.version() + " done: " + step.description() + ".");
            }
        } catch (SQLException failed) {
            throw new StorageException("Could not bring the database tables up to date", failed);
        }
    }

    /** The last step that ran, 0 for a new database. */
    int version(Connection connection, String schema) throws SQLException {
        try (Statement select = connection.createStatement();
             ResultSet result = select.executeQuery("SELECT MAX(version) FROM " + schema)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void setVersion(Connection connection, String schema, int version) throws SQLException {
        execute(connection, "DELETE FROM " + schema);
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + schema + " (version) VALUES (?)")) {
            insert.setInt(1, version);
            insert.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
