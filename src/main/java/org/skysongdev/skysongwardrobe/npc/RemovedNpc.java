package org.skysongdev.skysongwardrobe.npc;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One archived wardrobe of a removed NPC.
 *
 * @param entry     the archive entry's id, which keys its wardrobe
 * @param npc       the removed NPC's Citizens UUID
 * @param number    the NPC's Citizens number
 * @param name      the NPC's name
 * @param removedAt when it was removed
 */
public record RemovedNpc(UUID entry, UUID npc, int number, String name, Instant removedAt) {
    public RemovedNpc {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(removedAt, "removedAt");
    }
}
