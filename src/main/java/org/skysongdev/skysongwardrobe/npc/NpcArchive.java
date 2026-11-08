package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Keeps the wardrobes of removed NPCs in {@code npcs/removed/}, so their items are never lost.
 * Each removal gets its own entry id: {@code <entry>.yml} is a normal wardrobe file, so it opens
 * like any other, and {@code <entry>.info.yml} says which NPC it was and when it went.
 */
public final class NpcArchive {
    static final String INFO_SUFFIX = ".info.yml";

    private final Path folder;
    private final Logger logger;
    private final WardrobeRepository wardrobes;

    public NpcArchive(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
        this.wardrobes = new WardrobeRepository(folder, logger);
    }

    /** Stores a copy of the wardrobe of {@code npc}, removed at {@code when}; returns the new entry's id. */
    public UUID store(NpcInfo npc, Wardrobe wardrobe, Instant when) throws IOException {
        UUID entry = UUID.randomUUID();
        Wardrobe copy = wardrobes.load(entry);
        wardrobe.items().forEach(copy::put);
        await(wardrobes.save(copy));
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

    /** Every archived wardrobe, newest removal first. An entry whose info can't be read is skipped and kept. */
    public List<RemovedNpc> list() {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.getFileName().toString().endsWith(INFO_SUFFIX))
                    .flatMap(path -> read(path).stream())
                    .sorted(Comparator.comparing(RemovedNpc::removedAt).reversed())
                    .toList();
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not list " + folder + ".", failed);
            return List.of();
        }
    }

    /** Deletes the entry once its wardrobe is empty, so emptied entries don't pile up. */
    public void discardIfEmpty(UUID entry) throws IOException {
        if (wardrobes.load(entry).items().isEmpty()) {
            await(wardrobes.delete(entry));
            Files.deleteIfExists(info(entry));
        }
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

    /** The archive writes on the caller's thread, so the future is already done; this unwraps its failure. */
    private static void await(CompletableFuture<Void> write) throws IOException {
        try {
            write.join();
        } catch (CompletionException failed) {
            throw failed.getCause() instanceof IOException io ? io : new IOException(failed.getCause());
        }
    }

    Path info(UUID entry) {
        return folder.resolve(entry + INFO_SUFFIX);
    }
}
