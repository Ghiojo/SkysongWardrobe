package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStore;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStoreContract;

import java.nio.file.Path;
import java.util.logging.Logger;

/** {@link SqlRemovedNpcStore} on SQLite and on H2 in MySQL mode. */
class SqlRemovedNpcStoreTest {

    private static RemovedNpcStore migrated(SqlDatabase database) {
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        return new SqlRemovedNpcStore(database);
    }

    @Nested
    class OnSqlite extends RemovedNpcStoreContract {
        @TempDir
        Path folder;

        @Override
        protected RemovedNpcStore emptyStore() {
            return migrated(TestDatabases.sqlite(folder));
        }
    }

    @Nested
    class OnMySql extends RemovedNpcStoreContract {
        @Override
        protected RemovedNpcStore emptyStore() {
            return migrated(TestDatabases.mysql());
        }
    }
}
