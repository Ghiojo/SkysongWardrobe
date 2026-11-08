package org.skysongdev.skysongwardrobe.storage;

import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;

import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;
import org.skysongdev.skysongwardrobe.wardrobe.YamlWardrobeStore;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/** {@code storage.type: yaml}: the plain files in the plugin folder, exactly as before databases. */
public final class YamlStorage implements StorageBackend {
    public static final String TEXTURE_FILE = "texture-cache.yml";

    private final Path dataFolder;
    private final Logger logger;
    private final TextureCache textures;

    public YamlStorage(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.textures = YamlTextureCache.load(dataFolder.resolve(TEXTURE_FILE));
    }

    /** The folder each kind's {@code <uuid>.yml} files are in, inside the plugin folder. */
    public static Path wardrobeFolder(Path dataFolder, WardrobeKind kind) {
        return switch (kind) {
            case PLAYER -> dataFolder.resolve("wardrobes");
            case MANNEQUIN -> dataFolder.resolve("mannequins").resolve("wardrobes");
            case NPC -> dataFolder.resolve("npcs").resolve("wardrobes");
            case REMOVED_NPC -> dataFolder.resolve("npcs").resolve("removed");
        };
    }

    @Override
    public TextureCache textures() {
        return textures;
    }

    @Override
    public WardrobeStore wardrobes(WardrobeKind kind) {
        return new YamlWardrobeStore(wardrobeFolder(dataFolder, kind), logger);
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
