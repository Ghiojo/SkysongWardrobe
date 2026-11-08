package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps the wardrobes of removed NPCs, so their items are never lost. Each removal gets its own
 * entry id: its items are a normal wardrobe under that id, so it opens like any other, and the
 * {@link RemovedNpcStore} says which NPC it was and when it went.
 */
public final class NpcArchive {
    private final RemovedNpcStore entries;
    private final WardrobeRepository wardrobes;
    private final Logger logger;

    public NpcArchive(RemovedNpcStore entries, WardrobeRepository wardrobes, Logger logger) {
        this.entries = entries;
        this.wardrobes = wardrobes;
        this.logger = logger;
    }

    /** The archive in {@code folder}: wardrobe files beside their {@code <entry>.info.yml}. */
    public NpcArchive(Path folder, Logger logger) {
        this(new YamlRemovedNpcStore(folder, logger), new WardrobeRepository(folder, logger), logger);
    }

    /** Stores a copy of the wardrobe of {@code npc}, removed at {@code when}; returns the new entry's id. */
    public UUID store(NpcInfo npc, Wardrobe wardrobe, Instant when) throws IOException {
        UUID entry = UUID.randomUUID();
        Wardrobe copy = wardrobes.load(entry);
        wardrobe.items().forEach(copy::put);
        await(wardrobes.save(copy));
        entries.write(new RemovedNpc(entry, npc.id(), npc.number(), npc.name(), when));
        return entry;
    }

    /** The archived wardrobes, keyed by entry id. */
    public WardrobeRepository wardrobes() {
        return wardrobes;
    }

    /** Every archived wardrobe, newest removal first. An entry that can't be read is skipped and kept. */
    public List<RemovedNpc> list() {
        try {
            return entries.readAll().stream()
                    .sorted(Comparator.comparing(RemovedNpc::removedAt).reversed())
                    .toList();
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not list the removed NPCs.", failed);
            return List.of();
        }
    }

    /** Deletes the entry once its wardrobe is empty, so emptied entries don't pile up. */
    public void discardIfEmpty(UUID entry) throws IOException {
        if (wardrobes.load(entry).items().isEmpty()) {
            await(wardrobes.delete(entry));
            entries.delete(entry);
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
}
