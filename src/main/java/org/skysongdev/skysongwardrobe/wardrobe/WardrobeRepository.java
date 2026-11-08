package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.storage.AsyncWriter;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * One loaded {@link Wardrobe} per owner, kept until {@link #unload}, saved through a
 * {@link WardrobeStore}. Saves and deletes go to an {@link AsyncWriter}, so the main thread
 * never waits on disk or a database; reads go through the same writer, so they always see the
 * saves queued before them. An item that can't be read (say, after a downgrade) is kept as its
 * saved bytes and written back on every save, so saving never loses it.
 */
public final class WardrobeRepository {
    private final WardrobeStore store;
    private final AsyncWriter writer;
    private final Logger logger;
    private final Map<UUID, Wardrobe> loaded = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, byte[]>> unreadable = new ConcurrentHashMap<>();

    public WardrobeRepository(WardrobeStore store, AsyncWriter writer, Logger logger) {
        this.store = store;
        this.writer = writer;
        this.logger = logger;
    }

    /** Wardrobes saved as YAML files in {@code folder}, written on the caller's thread. */
    public WardrobeRepository(Path folder, Logger logger) {
        this(new YamlWardrobeStore(folder, logger), AsyncWriter.direct(), logger);
    }

    /**
     * The owner's wardrobe, read from the store the first time; empty if they have none yet.
     * A first read waits for the store, so players' wardrobes are read at login
     * ({@link WardrobeSessionListener}) and are already here by the time the main thread asks.
     * A store that can't be read throws {@link StorageException} rather than handing out an empty
     * wardrobe, whose next save would wipe the real one.
     */
    public Wardrobe load(UUID owner) {
        Wardrobe cached = loaded.get(owner);
        if (cached != null) {
            return cached;
        }
        Wardrobe read;
        try {
            read = writer.submit(() -> read(owner)).join();
        } catch (CompletionException failed) {
            throw new StorageException("Could not read the wardrobe of " + owner, failed.getCause());
        }
        Wardrobe raced = loaded.putIfAbsent(owner, read);
        return raced != null ? raced : read;
    }

    /** The owner's wardrobe if it is loaded; never waits. */
    public Optional<Wardrobe> loaded(UUID owner) {
        return Optional.ofNullable(loaded.get(owner));
    }

    /**
     * Queues a save of the wardrobe as it is now. The items are copied on the calling thread,
     * so later changes don't leak into this save. The future fails if the save did.
     */
    public CompletableFuture<Void> save(Wardrobe wardrobe) {
        Map<String, byte[]> slots = new LinkedHashMap<>(unreadable.getOrDefault(wardrobe.owner(), Map.of()));
        wardrobe.items().forEach((slot, item) -> slots.put(slot, item.serializeAsBytes()));
        return writer.submit(() -> {
            store.write(wardrobe.owner(), slots);
            return null;
        });
    }

    /**
     * Queues deleting the owner's wardrobe, for a broken mannequin whose items were given back. If
     * it still holds items that couldn't be read, the store keeps it aside instead, so they are never lost.
     */
    public CompletableFuture<Void> delete(UUID owner) {
        boolean keepAside = unreadable.containsKey(owner);
        loaded.remove(owner);
        unreadable.remove(owner);
        return writer.submit(() -> {
            store.delete(owner, keepAside);
            return null;
        });
    }

    /** Forgets the cached copy, for an owner who left; the next load reads the store again. */
    public void unload(UUID owner) {
        loaded.remove(owner);
        unreadable.remove(owner);
    }

    /** Completes once every save and delete queued so far is written. */
    public CompletableFuture<Void> flush() {
        return writer.flush();
    }

    private Wardrobe read(UUID owner) throws IOException {
        Wardrobe wardrobe = new Wardrobe(owner);
        Map<String, byte[]> broken = new HashMap<>();
        store.read(owner).forEach((slot, bytes) -> {
            try {
                wardrobe.put(slot, ItemStack.deserializeBytes(bytes));
            } catch (RuntimeException unreadableItem) {
                logger.warning("The item in slot " + slot + " of the wardrobe of " + owner
                        + " can't be read, so it is kept as saved.");
                broken.put(slot, bytes);
            }
        });
        if (!broken.isEmpty()) {
            unreadable.put(owner, broken);
        }
        return wardrobe;
    }
}
