package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class SqliteWardrobeStoreTest extends SqlWardrobeStoreContract {
    @TempDir
    Path folder;

    @Override
    SqlDatabase database() {
        return TestDatabases.sqlite(folder);
    }
}
