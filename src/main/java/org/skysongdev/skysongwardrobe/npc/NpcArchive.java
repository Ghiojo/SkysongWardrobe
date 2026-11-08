package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Keeps the wardrobes of removed NPCs in {@code npcs/removed/<uuid>-<seconds>/}, so their items
 * are never lost: {@code info.yml} says which NPC it was and when it went, and the folder holds
 * the wardrobe as a normal wardrobe file, so it opens like any other.
 */
public final class NpcArchive {
    static final String INFO = "info.yml";

    private final Path folder;
    private final Logger logger;

    public NpcArchive(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    /** Stores a copy of the wardrobe of {@code npc}, removed at {@code when}; returns its folder. */
    public Path store(NpcInfo npc, Wardrobe wardrobe, Instant when) throws IOException {
        Path entry = folder.resolve(npc.id() + "-" + when.getEpochSecond());
        Files.createDirectories(entry);
        Wardrobe copy = new Wardrobe(npc.id());
        wardrobe.items().forEach(copy::put);
        new WardrobeRepository(entry, logger).save(copy);
        YamlConfiguration info = new YamlConfiguration();
        info.set("npc", npc.id().toString());
        info.set("number", npc.number());
        info.set("name", npc.name());
        info.set("removed-at", when.toString());
        info.save(entry.resolve(INFO).toFile());
        return entry;
    }
}
