package org.skysongdev.skysongwardrobe.storage;

import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;

import java.nio.file.Path;
import java.util.List;

/**
 * Where all of SkysongWardrobe's stored data lives, picked once at start by {@code storage.type}.
 * Each kind of data gets its own store from here, so code that saves data never knows whether
 * it is writing files or a database. Skin galleries always stay as files and are not part of it.
 */
public interface StorageBackend extends AutoCloseable {

    /** The signed skins. */
    TextureCache textures();

    /** The wardrobes of one kind of wearer, kept apart from the other kinds. */
    WardrobeStore wardrobes(WardrobeKind kind);

    /**
     * What {@code /sw storage import} copies from the YAML files in {@code dataFolder} into this
     * backend; nothing for YAML itself.
     */
    List<DatasetImporter> importers(Path dataFolder);

    /** Releases the files or connections, when the plugin disables. */
    @Override
    void close();
}
