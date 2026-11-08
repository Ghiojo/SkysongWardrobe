package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Keeps each removed NPC entry in {@code <folder>/<entry>.info.yml}, beside its wardrobe file. A
 * file that can't be read is skipped with a warning and left alone.
 */
public final class YamlRemovedNpcStore implements RemovedNpcStore {
    static final String INFO_SUFFIX = ".info.yml";

    private final Path folder;
    private final Logger logger;

    public YamlRemovedNpcStore(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    @Override
    public List<RemovedNpc> readAll() throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.getFileName().toString().endsWith(INFO_SUFFIX))
                    .flatMap(path -> read(path).stream())
                    .toList();
        }
    }

    @Override
    public void write(RemovedNpc entry) throws IOException {
        YamlConfiguration info = new YamlConfiguration();
        info.set("npc", entry.npc().toString());
        info.set("number", entry.number());
        info.set("name", entry.name());
        info.set("removed-at", entry.removedAt().toString());
        Files.createDirectories(folder);
        info.save(file(entry.entry()).toFile());
    }

    @Override
    public void delete(UUID entry) throws IOException {
        Files.deleteIfExists(file(entry));
    }

    private Optional<RemovedNpc> read(Path path) {
        String name = path.getFileName().toString();
        try {
            UUID entry = UUID.fromString(name.substring(0, name.length() - INFO_SUFFIX.length()));
            YamlConfiguration info = YamlConfiguration.loadConfiguration(path.toFile());
            return Optional.of(new RemovedNpc(entry, UUID.fromString(info.getString("npc", "")), info.getInt("number"),
                    info.getString("name", "?"), Instant.parse(info.getString("removed-at", ""))));
        } catch (RuntimeException broken) {
            logger.warning(path + " can't be read, so it is left alone: " + broken.getMessage());
            return Optional.empty();
        }
    }

    Path file(UUID entry) {
        return folder.resolve(entry + INFO_SUFFIX);
    }
}
