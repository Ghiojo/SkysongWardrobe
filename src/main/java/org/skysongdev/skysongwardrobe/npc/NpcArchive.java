package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Keeps the wardrobes of removed NPCs in {@code npcs/removed/}, so their items are never lost.
 * Each removal gets its own entry id: {@code <entry>.yml} is a normal wardrobe file, so it opens
 * like any other, and {@code <entry>.info.yml} says which NPC it was and when it went.
 */
public final class NpcArchive {
    static final String INFO_SUFFIX = ".info.yml";

    private final Path folder;
    private final WardrobeRepository wardrobes;

    public NpcArchive(Path folder, Logger logger) {
        this.folder = folder;
        this.wardrobes = new WardrobeRepository(folder, logger);
    }

    /** Stores a copy of the wardrobe of {@code npc}, removed at {@code when}; returns the new entry's id. */
    public UUID store(NpcInfo npc, Wardrobe wardrobe, Instant when) throws IOException {
        UUID entry = UUID.randomUUID();
        Wardrobe copy = wardrobes.load(entry);
        wardrobe.items().forEach(copy::put);
        wardrobes.save(copy);
        YamlConfiguration info = new YamlConfiguration();
        info.set("npc", npc.id().toString());
        info.set("number", npc.number());
        info.set("name", npc.name());
        info.set("removed-at", when.toString());
        info.save(info(entry).toFile());
        return entry;
    }

    /** The archived wardrobes, keyed by entry id. */
    public WardrobeRepository wardrobes() {
        return wardrobes;
    }

    Path info(UUID entry) {
        return folder.resolve(entry + INFO_SUFFIX);
    }
}
