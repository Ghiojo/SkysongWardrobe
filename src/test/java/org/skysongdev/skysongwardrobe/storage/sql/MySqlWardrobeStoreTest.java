package org.skysongdev.skysongwardrobe.storage.sql;

class MySqlWardrobeStoreTest extends SqlWardrobeStoreContract {
    @Override
    SqlDatabase database() {
        return TestDatabases.mysql();
    }
}
