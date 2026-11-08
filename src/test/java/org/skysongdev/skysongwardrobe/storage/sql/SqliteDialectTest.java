package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class SqliteDialectTest extends SqlDialectContract {
    @TempDir
    Path folder;

    @Override
    SqlDatabase emptyDatabase() {
        return TestDatabases.sqlite(folder);
    }
}
