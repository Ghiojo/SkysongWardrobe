package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.render.RenderListener;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.util.Optional;
import java.util.UUID;

/**
 * Puts signed skins on NPCs, on the main thread. Citizens keeps the skin of a despawned NPC too,
 * so unlike mannequins nothing waits for the NPC to load. An NPC removed meanwhile is skipped.
 */
public final class NpcSkinApplier implements RenderListener {
    private final NpcSkins skins;
    private final TaskRunner tasks;

    public NpcSkinApplier(NpcSkins skins, TaskRunner tasks) {
        this.skins = skins;
        this.tasks = tasks;
    }

    @Override
    public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
        tasks.sync(() -> apply(wearer, texture));
    }

    @Override
    public void failed(UUID wearer, WearerKind kind, String reason) {
        // The queue logged it; the NPC keeps the skin it has.
    }

    /** Gives the NPC the texture unless it is gone or already wears it, since applying respawns it. Main thread only. */
    public void apply(UUID npc, SignedTexture texture) {
        if (skins.isDressable(npc) && !skins.current(npc).equals(Optional.of(texture))) {
            skins.apply(npc, texture);
        }
    }
}
