package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Saves mannequins to {@code <folder>/<uuid>.yml}, the spawner's bytes in Base64. */
public final class YamlMannequinStore implements MannequinStore {
    private static final String SUFFIX = ".yml";

    private final Path folder;
    private final Logger logger;

    public YamlMannequinStore(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    @Override
    public Contents readAll() {
        List<Row> rows = new ArrayList<>();
        Set<UUID> unreadable = new HashSet<>();
        if (!Files.isDirectory(folder)) {
            return new Contents(rows, unreadable);
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(path -> path.getFileName().toString().endsWith(SUFFIX))
                    .forEach(path -> read(path, rows, unreadable));
        } catch (IOException broken) {
            logger.severe("Could not list " + folder + ": " + broken.getMessage());
        }
        return new Contents(rows, unreadable);
    }

    private void read(Path path, List<Row> rows, Set<UUID> unreadable) {
        String name = path.getFileName().toString();
        UUID id;
        try {
            id = UUID.fromString(name.substring(0, name.length() - SUFFIX.length()));
        } catch (IllegalArgumentException notOurs) {
            return;
        }
        try {
            YamlConfiguration file = YamlConfiguration.loadConfiguration(path.toFile());
            rows.add(new Row(id, UUID.fromString(file.getString("owner", "")), file.getString("skin", ""),
                    file.getString("model", ""), Base64.getDecoder().decode(file.getString("spawner", ""))));
        } catch (RuntimeException broken) {
            logger.warning(path + " can't be read, so it is kept untouched and its mannequin left alone: "
                    + broken.getMessage());
            unreadable.add(id);
        }
    }

    @Override
    public void write(Row row) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set("owner", row.owner().toString());
        file.set("skin", row.skin());
        file.set("model", row.model());
        file.set("spawner", Base64.getEncoder().encodeToString(row.spawner()));
        Files.createDirectories(folder);
        file.save(file(row.id()).toFile());
    }

    @Override
    public void delete(UUID id) throws IOException {
        Files.deleteIfExists(file(id));
    }

    private Path file(UUID id) {
        return folder.resolve(id + SUFFIX);
    }
}
