package org.skysongdev.skysongwardrobe.storage.sql;

import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.storage.StorageBackend;
import org.skysongdev.skysongwardrobe.storage.YamlStorage;
import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.storage.importing.TextureImporter;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@code storage.type: sqlite} or {@code mysql}: brings the tables up to date on start, then
 * hands out the SQL stores, all sharing one connection pool.
 */
public final class SqlStorage implements StorageBackend {
    private final AutoCloseable connections;
    private final TextureCache textures;

    /** Takes ownership of {@code connections}, which {@link #close()} closes. */
    public SqlStorage(SqlDatabase database, AutoCloseable connections, Clock clock, Logger logger) {
        this.connections = connections;
        new SchemaMigrator(database, logger).migrate(WardrobeSchema.steps());
        this.textures = new SqlTextureCache(database, clock);
    }

    @Override
    public TextureCache textures() {
        return textures;
    }

    @Override
    public List<DatasetImporter> importers(Path dataFolder) {
        return List.of(new TextureImporter(dataFolder.resolve(YamlStorage.TEXTURE_FILE), textures));
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
