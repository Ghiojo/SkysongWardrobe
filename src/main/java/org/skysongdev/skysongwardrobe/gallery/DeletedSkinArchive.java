package org.skysongdev.skysongwardrobe.gallery;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The {@code deleted/} folder: a copy of every deleted entry's image, named
 * {@code <player>-<id>.png}, then {@code <player>-<id>_2.png} and so on when that is taken.
 * The image also stays in the player's folder, so items that carry it still render.
 */
public final class DeletedSkinArchive {
    private final Path folder;

    public DeletedSkinArchive(Path folder) {
        this.folder = folder;
    }

    /**
     * Copies the entry's image here, then removes the entry from the gallery.
     *
     * @return the copy, or empty if the gallery had no such entry
     */
    public Optional<Path> delete(PlayerGallery gallery, SkinId id) throws IOException {
        if (gallery.entry(id).isEmpty()) {
            return Optional.empty();
        }
        Optional<Path> copy = archive(gallery, id);
        gallery.remove(id);
        return copy;
    }

    /** Copies the entry's current image here and changes nothing else; empty if it has no image. */
    public Optional<Path> archive(PlayerGallery gallery, SkinId id) throws IOException {
        Path image = gallery.imageFile(id);
        if (!Files.isRegularFile(image)) {
            return Optional.empty();
        }
        return Optional.of(copy(image, gallery.folder().getFileName() + "-" + id.value()));
    }

    private Path copy(Path image, String baseName) throws IOException {
        Files.createDirectories(folder);
        for (int attempt = 1; ; attempt++) {
            String suffix = attempt == 1 ? "" : "_" + attempt;
            Path target = folder.resolve(baseName + suffix + PlayerGallery.IMAGE_EXTENSION);
            try {
                // Without REPLACE_EXISTING, so two deletes at once never overwrite each other.
                return Files.copy(image, target);
            } catch (FileAlreadyExistsException taken) {
                // Try the next number.
            }
        }
    }
}
