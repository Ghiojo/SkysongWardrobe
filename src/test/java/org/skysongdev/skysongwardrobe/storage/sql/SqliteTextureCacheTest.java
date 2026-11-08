package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class SqliteTextureCacheTest extends SqlTextureCacheContract {
    @TempDir
    Path folder;

    @Override
    SqlDatabase database() {
        return TestDatabases.sqlite(folder);
    }
}
