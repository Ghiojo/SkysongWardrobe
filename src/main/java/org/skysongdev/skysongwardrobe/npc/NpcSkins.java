package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Optional;
import java.util.UUID;

/**
 * The skins of Citizens NPCs, so nothing outside the Citizens adapter touches Citizens. Every
 * method runs on the main thread.
 */
public interface NpcSkins {
    /** Whether the NPC exists and is a player NPC, the only kind that can be dressed. */
    boolean isDressable(UUID npc);

    /** The NPC's skin texture right now; empty if it has none yet. */
    Optional<SignedTexture> current(UUID npc);

    /** Gives the NPC this skin for good, so Citizens saves it and never fetches another. */
    void apply(UUID npc, SignedTexture texture);
}
