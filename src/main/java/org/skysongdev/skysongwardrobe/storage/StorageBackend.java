package org.skysongdev.skysongwardrobe.storage;

import org.skysongdev.skysongwardrobe.render.TextureCache;

/**
 * Where all of SkysongWardrobe's stored data lives, picked once at start by {@code storage.type}.
 * Each kind of data gets its own store from here, so code that saves data never knows whether
 * it is writing files or a database. Skin galleries always stay as files and are not part of it.
 */
public interface StorageBackend extends AutoCloseable {

    /** The signed skins. */
    TextureCache textures();

    /** Releases the files or connections, when the plugin disables. */
    @Override
    void close();
}
