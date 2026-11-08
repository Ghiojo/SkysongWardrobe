package org.skysongdev.skysongwardrobe.storage.importing;

import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;
import org.skysongdev.skysongwardrobe.wardrobe.YamlWardrobeStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Copies one kind's {@code <uuid>.yml} wardrobe files into the database. An owner the database
 * already has a wardrobe for is skipped, since that one is newer. {@code <uuid>.broken.yml}
 * files, and slots whose text isn't even Base64, stay in the files only and are logged.
 */
public final class WardrobeImporter implements DatasetImporter {
    private static final String SUFFIX = ".yml";

    private final WardrobeKind kind;
    private final Path folder;
    private final WardrobeStore target;
    private final Logger logger;

    public WardrobeImporter(WardrobeKind kind, Path folder, WardrobeStore target, Logger logger) {
        this.kind = kind;
        this.folder = folder;
        this.target = target;
        this.logger = logger;
    }

    @Override
    public String dataset() {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', ' ') + " wardrobes";
    }

    @Override
    public ImportResult run() throws IOException {
        YamlWardrobeStore files = new YamlWardrobeStore(folder, logger);
        int imported = 0;
        int skipped = 0;
        for (UUID owner : owners()) {
            Map<String, byte[]> slots = files.read(owner);
            if (files.keepsUndecodable(owner)) {
                logger.warning(folder.resolve(owner + SUFFIX) + " has slots that can't be read; they stay in the file"
                        + " and are not imported.");
            }
            if (slots.isEmpty() || !target.read(owner).isEmpty()) {
                skipped++;
            } else {
                target.write(owner, slots);
                imported++;
            }
        }
        return new ImportResult(dataset(), imported, skipped);
    }

    private List<UUID> owners() throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(folder)) {
            return paths.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(SUFFIX))
                    .flatMap(name -> owner(name).stream())
                    .toList();
        }
    }

    /** The owner a file belongs to; nothing for broken.yml files and anything else in the folder. */
    private Optional<UUID> owner(String fileName) {
        String name = fileName.substring(0, fileName.length() - SUFFIX.length());
        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException notAnOwner) {
            if (name.endsWith(".broken")) {
                logger.warning(folder.resolve(fileName) + " holds items that can't be read; it stays a file and is"
                        + " not imported.");
            }
            return Optional.empty();
        }
    }
}
