package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves NPC records to {@code npcs/<uuid>.yml}. A server can have many NPCs, and only those with a
 * wardrobe have a file, so each is read the first time it is asked for and then kept. A file
 * that can't be read is left untouched and treated as missing, so nothing in it is lost.
 */
public final class NpcRepository {
    private static final String VALUE = "base.value";
    private static final String SIGNATURE = "base.signature";

    private final Path folder;
    private final Logger logger;
    private final Map<UUID, Optional<NpcRecord>> cache = new HashMap<>();

    public NpcRepository(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    /** The NPC's record, if it has one. */
    public synchronized Optional<NpcRecord> find(UUID id) {
        return cache.computeIfAbsent(id, this::read);
    }

    public synchronized void save(NpcRecord npc) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set(VALUE, npc.base().value());
        file.set(SIGNATURE, npc.base().signature());
        Files.createDirectories(folder);
        file.save(file(npc.id()).toFile());
        cache.put(npc.id(), Optional.of(npc));
    }

    public synchronized void delete(UUID id) throws IOException {
        Files.deleteIfExists(file(id));
        cache.put(id, Optional.empty());
    }

    /** Where the NPC's record is saved, for moving it aside when the NPC is removed. */
    public Path file(UUID id) {
        return folder.resolve(id + ".yml");
    }

    /** Forgets the cached copy, for a record whose file was moved; the next find reads the disk again. */
    public synchronized void forget(UUID id) {
        cache.remove(id);
    }

    private Optional<NpcRecord> read(UUID id) {
        Path path = file(id);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        YamlConfiguration file = YamlConfiguration.loadConfiguration(path.toFile());
        String value = file.getString(VALUE);
        String signature = file.getString(SIGNATURE);
        if (value == null || signature == null) {
            logger.warning(path + " has no base skin, so it is kept untouched and the NPC is treated as undressed.");
            return Optional.empty();
        }
        return Optional.of(new NpcRecord(id, new SignedTexture(value, signature)));
    }
}
