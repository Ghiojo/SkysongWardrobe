package org.skysongdev.skysongwardrobe.storage;

import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;

import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;

import java.nio.file.Path;
import java.util.List;

/** {@code storage.type: yaml}: the plain files in the plugin folder, exactly as before databases. */
public final class YamlStorage implements StorageBackend {
    public static final String TEXTURE_FILE = "texture-cache.yml";

    private final TextureCache textures;

    public YamlStorage(Path dataFolder) {
        this.textures = YamlTextureCache.load(dataFolder.resolve(TEXTURE_FILE));
    }

    @Override
    public TextureCache textures() {
        return textures;
    }

    /** The data already is in the files. */
    @Override
    public List<DatasetImporter> importers(Path dataFolder) {
        return List.of();
    }

    /** Every file is written as it changes, so there is nothing left to close. */
    @Override
    public void close() {
    }
}
