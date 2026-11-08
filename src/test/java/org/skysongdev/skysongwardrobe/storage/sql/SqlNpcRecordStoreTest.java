package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStore;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStoreContract;

import java.nio.file.Path;
import java.util.logging.Logger;

/** {@link SqlNpcRecordStore} on SQLite and on H2 in MySQL mode. */
class SqlNpcRecordStoreTest {

    private static NpcRecordStore migrated(SqlDatabase database) {
        new SchemaMigrator(database, Logger.getAnonymousLogger()).migrate(WardrobeSchema.steps());
        return new SqlNpcRecordStore(database);
    }

    @Nested
    class OnSqlite extends NpcRecordStoreContract {
        @TempDir
        Path folder;

        @Override
        protected NpcRecordStore emptyStore() {
            return migrated(TestDatabases.sqlite(folder));
        }
    }

    @Nested
    class OnMySql extends NpcRecordStoreContract {
        @Override
        protected NpcRecordStore emptyStore() {
            return migrated(TestDatabases.mysql());
        }
    }
}
