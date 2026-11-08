package org.skysongdev.skysongwardrobe.storage.sql;

class MySqlTextureCacheTest extends SqlTextureCacheContract {
    @Override
    SqlDatabase database() {
        return TestDatabases.mysql();
    }
}
