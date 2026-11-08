package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.ArmModel;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Every mannequin, read from a {@link MannequinStore} at start, so lookups and per-player counts
 * never wait on the store. A record that can't be read is left untouched and its id remembered,
 * so its entity is never treated as an orphan and nothing in it is lost.
 */
public final class MannequinRepository {
    private final MannequinStore store;
    private final Map<UUID, Mannequin> mannequins = new HashMap<>();
    private final Set<UUID> unreadable = new HashSet<>();

    private MannequinRepository(MannequinStore store) {
        this.store = store;
    }

    /** A repository holding every mannequin saved in the store. */
    public static MannequinRepository load(MannequinStore store, Logger logger) {
        MannequinRepository repository = new MannequinRepository(store);
        MannequinStore.Contents contents;
        try {
            contents = store.readAll();
        } catch (IOException failed) {
            throw new StorageException("Could not read the mannequins", failed);
        }
        repository.unreadable.addAll(contents.unreadable());
        for (MannequinStore.Row row : contents.rows()) {
            try {
                repository.mannequins.put(row.id(), new Mannequin(row.id(), row.owner(),
                        ItemStack.deserializeBytes(row.spawner()), row.skin(), ArmModel.valueOf(row.model())));
            } catch (RuntimeException broken) {
                logger.warning("Mannequin " + row.id() + " can't be read, so its record is kept untouched and its"
                        + " mannequin left alone: " + broken.getMessage());
                repository.unreadable.add(row.id());
            }
        }
        return repository;
    }

    /** A repository holding every mannequin saved as YAML in {@code folder}. */
    public static MannequinRepository load(Path folder, Logger logger) {
        return load(new YamlMannequinStore(folder, logger), logger);
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

    /** Saves the mannequin; it counts as placed only once the store has it. */
    public synchronized void save(Mannequin mannequin) throws IOException {
        store.write(new MannequinStore.Row(mannequin.id(), mannequin.owner(), mannequin.skin(), mannequin.model().name(),
                mannequin.spawner().serializeAsBytes()));
        mannequins.put(mannequin.id(), mannequin);
    }

    public synchronized void delete(UUID id) throws IOException {
        store.delete(id);
        mannequins.remove(id);
        unreadable.remove(id);
    }
}
