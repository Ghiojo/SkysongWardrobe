package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.mannequin.MannequinStore;
import org.skysongdev.skysongwardrobe.mannequin.YamlMannequinStore;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStore;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStore;
import org.skysongdev.skysongwardrobe.npc.YamlRemovedNpcStore;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.storage.StorageBackend;
import org.skysongdev.skysongwardrobe.storage.YamlStorage;
import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.storage.importing.MannequinImporter;
import org.skysongdev.skysongwardrobe.storage.importing.NpcRecordImporter;
import org.skysongdev.skysongwardrobe.storage.importing.RemovedNpcImporter;
import org.skysongdev.skysongwardrobe.storage.importing.TextureImporter;
import org.skysongdev.skysongwardrobe.storage.importing.WardrobeImporter;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@code storage.type: sqlite} or {@code mysql}: brings the tables up to date on start, then
 * hands out the SQL stores, all sharing one connection pool.
 */
public final class SqlStorage implements StorageBackend {
    private final SqlDatabase database;
    private final AutoCloseable connections;
    private final Logger logger;
    private final TextureCache textures;

    /** Takes ownership of {@code connections}, which {@link #close()} closes. */
    public SqlStorage(SqlDatabase database, AutoCloseable connections, Clock clock, Logger logger) {
        this.database = database;
        this.connections = connections;
        this.logger = logger;
        new SchemaMigrator(database, logger).migrate(WardrobeSchema.steps());
        this.textures = new SqlTextureCache(database, clock);
    }

    @Override
    public TextureCache textures() {
        return textures;
    }

    @Override
    public WardrobeStore wardrobes(WardrobeKind kind) {
        return new SqlWardrobeStore(database, kind);
    }

    @Override
    public MannequinStore mannequins() {
        return new SqlMannequinStore(database);
    }

    @Override
    public NpcRecordStore npcRecords() {
        return new SqlNpcRecordStore(database);
    }

    @Override
    public RemovedNpcStore removedNpcs() {
        return new SqlRemovedNpcStore(database);
    }

    @Override
    public List<DatasetImporter> importers(Path dataFolder) {
        return List.of(
                new TextureImporter(dataFolder.resolve(YamlStorage.TEXTURE_FILE), textures),
                wardrobeImporter(WardrobeKind.PLAYER, dataFolder),
                wardrobeImporter(WardrobeKind.MANNEQUIN, dataFolder),
                wardrobeImporter(WardrobeKind.NPC, dataFolder),
                new MannequinImporter(new YamlMannequinStore(YamlStorage.mannequinFolder(dataFolder), logger),
                        mannequins()),
                new NpcRecordImporter(YamlStorage.npcFolder(dataFolder), npcRecords(), logger),
                wardrobeImporter(WardrobeKind.REMOVED_NPC, dataFolder),
                new RemovedNpcImporter(new YamlRemovedNpcStore(
                        YamlStorage.wardrobeFolder(dataFolder, WardrobeKind.REMOVED_NPC), logger), removedNpcs()));
    }

    private DatasetImporter wardrobeImporter(WardrobeKind kind, Path dataFolder) {
        return new WardrobeImporter(kind, YamlStorage.wardrobeFolder(dataFolder, kind), wardrobes(kind), logger);
    }

    @Override
    public void close() {
        try {
            connections.close();
        } catch (Exception ignored) {
            // The plugin is stopping; a pool that fails to close takes nothing with it.
        }
    }
}
