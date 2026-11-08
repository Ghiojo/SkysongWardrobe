package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * NPC records, saved through a {@link NpcRecordStore}. A server can have many NPCs, and only
 * those with a wardrobe have a record, so each is read the first time it is asked for and then kept.
 */
public final class NpcRepository {
    private final NpcRecordStore store;
    private final Map<UUID, Optional<NpcRecord>> cache = new HashMap<>();

    public NpcRepository(NpcRecordStore store) {
        this.store = store;
    }

    /** Records saved as YAML files in {@code folder}. */
    public NpcRepository(Path folder, Logger logger) {
        this(new YamlNpcRecordStore(folder, logger));
    }

    /** The NPC's record, if it has one. A store that can't be read throws {@link StorageException}. */
    public synchronized Optional<NpcRecord> find(UUID id) {
        Optional<NpcRecord> cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        try {
            Optional<NpcRecord> read = store.read(id);
            cache.put(id, read);
            return read;
        } catch (IOException failed) {
            throw new StorageException("Could not read the record of NPC " + id, failed);
        }
    }

    public synchronized void save(NpcRecord npc) throws IOException {
        store.write(npc);
        cache.put(npc.id(), Optional.of(npc));
    }

    public synchronized void delete(UUID id) throws IOException {
        store.delete(id);
        cache.put(id, Optional.empty());
    }

    /** Forgets the cached copy; the next find reads the store again. */
    public synchronized void forget(UUID id) {
        cache.remove(id);
    }
}
