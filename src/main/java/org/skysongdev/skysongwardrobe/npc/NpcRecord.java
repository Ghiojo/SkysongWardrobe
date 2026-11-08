package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Objects;
import java.util.UUID;

/**
 * What the plugin remembers about one Citizens NPC with a wardrobe. What it wears lives in its
 * wardrobe, keyed by the same id; Citizens keeps everything else about the NPC.
 *
 * @param id   the NPC's Citizens UUID, which stays the same across restarts
 * @param base the skin the NPC had before it was dressed, drawn under every outfit. Kept here
 *             because once dressed, the NPC itself only holds the rendered skin
 */
public record NpcRecord(UUID id, SignedTexture base) {
    public NpcRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(base, "base");
    }

    /** The same NPC on a new base skin. */
    public NpcRecord withBase(SignedTexture newBase) {
        return new NpcRecord(id, newBase);
    }

    /** This record for another NPC, for one copied from this one. */
    public NpcRecord copyFor(UUID copy) {
        return new NpcRecord(copy, base);
    }
}
