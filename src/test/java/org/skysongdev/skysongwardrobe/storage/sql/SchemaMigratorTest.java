package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaMigratorTest {
    @TempDir
    Path folder;

    private final List<Integer> ran = new ArrayList<>();

    private SchemaStep step(int version) {
        return new SchemaStep(version, "step " + version, database -> {
            ran.add(version);
            return List.of("CREATE TABLE " + database.table("t" + version) + " (id INT)");
        });
    }

    private static int version(SqlDatabase database) throws SQLException {
        SchemaMigrator migrator = new SchemaMigrator(database, Logger.getAnonymousLogger());
        try (Connection connection = database.dataSource().getConnection()) {
            return migrator.version(connection, database.table("schema"));
        }
    }

    @Test
    void runsEveryStepOnceInOrder() throws SQLException {
        SqlDatabase database = TestDatabases.sqlite(folder);
        SchemaMigrator migrator = new SchemaMigrator(database, Logger.getAnonymousLogger());

        migrator.migrate(List.of(step(2), step(1)));
        migrator.migrate(List.of(step(1), step(2)));

        assertEquals(List.of(1, 2), ran);
        assertEquals(2, version(database));
    }

    @Test
    void aNewerPluginRunsOnlyItsNewSteps() throws SQLException {
        SqlDatabase database = TestDatabases.mysql();
        SchemaMigrator migrator = new SchemaMigrator(database, Logger.getAnonymousLogger());
        migrator.migrate(List.of(step(1)));

        migrator.migrate(List.of(step(1), step(2), step(3)));

        assertEquals(List.of(1, 2, 3), ran);
        assertEquals(3, version(database));
    }

    @Test
    void aNewDatabaseIsVersionZero() throws SQLException {
        SqlDatabase database = TestDatabases.sqlite(folder);
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(List.of());

        assertEquals(0, version(database));
    }

    @Test
    void aFailingStepIsAStorageErrorAndIsTriedAgainNextTime() throws SQLException {
        SqlDatabase database = TestDatabases.sqlite(folder);
        SchemaMigrator migrator = new SchemaMigrator(database, Logger.getAnonymousLogger());
        SchemaStep broken = new SchemaStep(1, "broken", ignored -> List.of("NOT SQL"));

        assertThrows(StorageException.class, () -> migrator.migrate(List.of(broken)));

        assertEquals(0, version(database));
    }

    @Test
    void theRealSchemaRunsOnBothDatabases() {
        for (SqlDatabase database : List.of(TestDatabases.sqlite(folder), TestDatabases.mysql())) {
            new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        }
    }

    @Test
    void versionsStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new SchemaStep(0, "zero", database -> List.of()));
    }
}
