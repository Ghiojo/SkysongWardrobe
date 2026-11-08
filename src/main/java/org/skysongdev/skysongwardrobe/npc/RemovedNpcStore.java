package org.skysongdev.skysongwardrobe.npc;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Where the removed-NPC archive keeps who each entry was; the entries' items live in their wardrobes. */
public interface RemovedNpcStore {

    /** Every entry that can be read, in no particular order. */
    List<RemovedNpc> readAll() throws IOException;

    /** Saves {@code entry}, replacing one with the same entry id. */
    void write(RemovedNpc entry) throws IOException;

    /** Forgets the entry; one that isn't there is fine. */
    void delete(UUID entry) throws IOException;
}
