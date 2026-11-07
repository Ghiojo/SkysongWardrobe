package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Saves mannequins to {@code mannequins/<uuid>.yml}, the spawner as Paper's serialized item bytes
 * in Base64. Every file is read at start, so lookups and per-player counts need no disk access.
 * A file that can't be read is left untouched and its id remembered, so its entity is never
 * treated as an orphan and nothing in it is lost.
 */
public final class MannequinRepository {
    private static final String SUFFIX = ".yml";

    private final Path folder;
    private final Logger logger;
    private final Map<UUID, Mannequin> mannequins = new HashMap<>();
    private final Set<UUID> unreadable = new HashSet<>();

    private MannequinRepository(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    /** A repository holding every mannequin saved in {@code folder}. */
    public static MannequinRepository load(Path folder, Logger logger) {
        MannequinRepository repository = new MannequinRepository(folder, logger);
        repository.readAll();
        return repository;
    }

    public synchronized Optional<Mannequin> find(UUID id) {
        return Optional.ofNullable(mannequins.get(id));
    }

    /** Whether the id belongs to a mannequin, readable or not. */
    public synchronized boolean isKnown(UUID id) {
        return mannequins.containsKey(id) || unreadable.contains(id);
    }

    /** How many readable mannequins the player has placed. */
    public synchronized int countOwnedBy(UUID owner) {
        return (int) mannequins.values().stream().filter(mannequin -> mannequin.owner().equals(owner)).count();
    }

    public synchronized void save(Mannequin mannequin) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set("owner", mannequin.owner().toString());
        file.set("skin", mannequin.skin());
        file.set("model", mannequin.model().name());
        file.set("spawner", Base64.getEncoder().encodeToString(mannequin.spawner().serializeAsBytes()));
        Files.createDirectories(folder);
        file.save(file(mannequin.id()).toFile());
        mannequins.put(mannequin.id(), mannequin);
    }

    public synchronized void delete(UUID id) throws IOException {
        Files.deleteIfExists(file(id));
        mannequins.remove(id);
        unreadable.remove(id);
    }

    private void readAll() {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(path -> path.getFileName().toString().endsWith(SUFFIX)).forEach(this::read);
        } catch (IOException broken) {
            logger.severe("Could not list " + folder + ": " + broken.getMessage());
        }
    }

    private void read(Path path) {
        String name = path.getFileName().toString();
        UUID id;
        try {
            id = UUID.fromString(name.substring(0, name.length() - SUFFIX.length()));
        } catch (IllegalArgumentException notOurs) {
            return;
        }
        try {
            YamlConfiguration file = YamlConfiguration.loadConfiguration(path.toFile());
            ItemStack spawner = ItemStack.deserializeBytes(Base64.getDecoder().decode(file.getString("spawner", "")));
            mannequins.put(id, new Mannequin(id, UUID.fromString(file.getString("owner", "")), spawner,
                    file.getString("skin", ""), ArmModel.valueOf(file.getString("model", ""))));
        } catch (RuntimeException broken) {
            logger.warning(path + " can't be read, so it is kept untouched and its mannequin left alone: "
                    + broken.getMessage());
            unreadable.add(id);
        }
    }

    private Path file(UUID id) {
        return folder.resolve(id + SUFFIX);
    }
}
