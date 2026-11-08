package org.skysongdev.skysongwardrobe.npc;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/** Reads and writes NPC records; {@link NpcRepository} caches them. Implementations may block on disk or a database. */
public interface NpcRecordStore {

    /** The NPC's record, empty when it has none or it can't be read (an unreadable one is left as it is). */
    Optional<NpcRecord> read(UUID id) throws IOException;

    void write(NpcRecord record) throws IOException;

    void delete(UUID id) throws IOException;
}
