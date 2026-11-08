package org.skysongdev.skysongwardrobe.wardrobe;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Reads and writes the saved slots of wardrobes, each item as Paper's serialized item bytes.
 * It knows nothing about items or menus: {@link WardrobeRepository} turns the bytes into items,
 * caches them and keeps the ones that can't be read. Implementations may block on disk or a
 * database.
 */
public interface WardrobeStore {

    /** The owner's saved slots, slot id to item bytes; empty when they have nothing saved. */
    Map<String, byte[]> read(UUID owner) throws IOException;

    /** Replaces the owner's saved slots with exactly {@code slots}. */
    void write(UUID owner, Map<String, byte[]> slots) throws IOException;

    /**
     * Deletes the owner's wardrobe. With {@code keepAside}, because some of its items can't be
     * read, it is moved somewhere it is never lost instead, and the owner reads as empty after.
     */
    void delete(UUID owner, boolean keepAside) throws IOException;
}
