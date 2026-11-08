package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * One loaded {@link Wardrobe} per owner, kept until {@link #unload}, saved through a
 * {@link WardrobeStore}. An item that can't be read (say, after a downgrade) is kept as its saved
 * bytes and written back on every save, so saving never loses it.
 */
public final class WardrobeRepository {
    private final WardrobeStore store;
    private final Logger logger;
    private final Map<UUID, Wardrobe> loaded = new HashMap<>();
    private final Map<UUID, Map<String, byte[]>> unreadable = new HashMap<>();

    public WardrobeRepository(WardrobeStore store, Logger logger) {
        this.store = store;
        this.logger = logger;
    }

    /** Wardrobes saved as YAML files in {@code folder}. */
    public WardrobeRepository(Path folder, Logger logger) {
        this(new YamlWardrobeStore(folder, logger), logger);
    }

    /**
     * The owner's wardrobe, read from the store the first time; empty if they have none yet. A
     * store that can't be read throws {@link StorageException} rather than handing out an empty
     * wardrobe, whose next save would wipe the real one.
     */
    public synchronized Wardrobe load(UUID owner) {
        return loaded.computeIfAbsent(owner, this::read);
    }

    public synchronized void save(Wardrobe wardrobe) throws IOException {
        Map<String, byte[]> slots = new LinkedHashMap<>(unreadable.getOrDefault(wardrobe.owner(), Map.of()));
        wardrobe.items().forEach((slot, item) -> slots.put(slot, item.serializeAsBytes()));
        store.write(wardrobe.owner(), slots);
    }

    /**
     * Deletes the owner's wardrobe, for a broken mannequin whose items were given back. If it
     * still holds items that couldn't be read, the store keeps it aside instead, so they are never lost.
     */
    public synchronized void delete(UUID owner) throws IOException {
        store.delete(owner, unreadable.containsKey(owner));
        loaded.remove(owner);
        unreadable.remove(owner);
    }

    /** Forgets the cached copy, for an owner who left; the next load reads the store again. */
    public synchronized void unload(UUID owner) {
        loaded.remove(owner);
        unreadable.remove(owner);
    }

    private Wardrobe read(UUID owner) {
        Wardrobe wardrobe = new Wardrobe(owner);
        Map<String, byte[]> slots;
        try {
            slots = store.read(owner);
        } catch (IOException failed) {
            throw new StorageException("Could not read the wardrobe of " + owner, failed);
        }
        slots.forEach((slot, bytes) -> {
            try {
                wardrobe.put(slot, ItemStack.deserializeBytes(bytes));
            } catch (RuntimeException broken) {
                logger.warning("The item in slot " + slot + " of the wardrobe of " + owner
                        + " can't be read, so it is kept as saved.");
                unreadable.computeIfAbsent(owner, key -> new HashMap<>()).put(slot, bytes);
            }
        });
        return wardrobe;
    }
}
