package org.skysongdev.skysongwardrobe.gallery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Hands out one {@link PlayerGallery} per owner, so two commands on the same gallery never
 * overwrite each other's changes. A cached gallery is dropped as soon as {@link GalleryFolders}
 * says its folder moved.
 */
public final class GalleryRepository {
    private final GalleryFolders folders;
    private final Logger logger;
    private final Map<UUID, PlayerGallery> galleries = new HashMap<>();

    public GalleryRepository(GalleryFolders folders, Logger logger) {
        this.folders = folders;
        this.logger = logger;
    }

    /**
     * The owner's gallery under their current name, renaming folders if needed. May block on
     * a Mojang lookup (see {@link GalleryFolders#claim}), so call it off the main thread.
     */
    public synchronized PlayerGallery claim(UUID owner, String name) throws IOException {
        return cached(owner, folders.claim(owner, name));
    }

    /** The owner's gallery if they have a folder; never blocks. */
    public synchronized Optional<PlayerGallery> existing(UUID owner) {
        return folders.existing(owner).map(folder -> cached(owner, folder));
    }

    private PlayerGallery cached(UUID owner, Path folder) {
        PlayerGallery gallery = galleries.get(owner);
        if (gallery == null || !gallery.folder().equals(folder)) {
            gallery = PlayerGallery.load(folder, owner, logger);
            galleries.put(owner, gallery);
        }
        return gallery;
    }
}
