package org.skysongdev.skysongwardrobe.mannequin;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Reads and writes mannequin records as plain values; {@link MannequinRepository} turns them into
 * {@link Mannequin}s. Implementations may block on disk or a database.
 */
public interface MannequinStore {

    /**
     * One saved mannequin, as stored.
     *
     * @param model   the {@link org.skysongdev.skysongwardrobe.render.ArmModel} name
     * @param spawner Paper's serialized bytes of the item that placed it
     */
    record Row(UUID id, UUID owner, String skin, String model, byte[] spawner) {
        public Row {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(skin, "skin");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(spawner, "spawner");
        }
    }

    /**
     * Everything saved.
     *
     * @param unreadable ids whose record can't even be read; they are left untouched
     */
    record Contents(List<Row> rows, Set<UUID> unreadable) {
        public Contents {
            rows = List.copyOf(rows);
            unreadable = Set.copyOf(unreadable);
        }
    }

    /** Every saved mannequin, read at start. */
    Contents readAll() throws IOException;

    /** Saves the mannequin, replacing the record with the same id. */
    void write(Row row) throws IOException;

    void delete(UUID id) throws IOException;
}
