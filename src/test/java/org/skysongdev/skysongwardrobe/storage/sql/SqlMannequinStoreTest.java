package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.mannequin.MannequinStore;
import org.skysongdev.skysongwardrobe.mannequin.MannequinStoreContract;

import java.nio.file.Path;
import java.util.logging.Logger;

/** {@link SqlMannequinStore} on SQLite and on H2 in MySQL mode. */
class SqlMannequinStoreTest {

    private static MannequinStore migrated(SqlDatabase database) {
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        return new SqlMannequinStore(database);
    }

    @Nested
    class OnSqlite extends MannequinStoreContract {
        @TempDir
        Path folder;

        @Override
        protected MannequinStore emptyStore() {
            return migrated(TestDatabases.sqlite(folder));
        }
    }

    @Nested
    class OnMySql extends MannequinStoreContract {
        @Override
        protected MannequinStore emptyStore() {
            return migrated(TestDatabases.mysql());
        }
    }
}
