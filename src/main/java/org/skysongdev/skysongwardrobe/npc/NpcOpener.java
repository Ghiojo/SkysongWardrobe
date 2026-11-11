package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Optional;

/**
 * Opens a Citizens NPC's wardrobe: only player NPCs wear one, and {@link NpcAccess} decides who
 * may open it unless the caller already decided. Main thread only.
 */
public final class NpcOpener {
    private final NpcSkins skins;
    private final NpcAccess access;
    private final WardrobeOpener opener;

    public NpcOpener(NpcSkins skins, NpcAccess access, WardrobeOpener opener) {
        this.skins = skins;
        this.access = access;
        this.opener = opener;
    }

    /**
     * Shows {@code viewer} the wardrobe of {@code target}.
     *
     * @param checkAccess false when the caller already decided the viewer may open it
     */
    public OpenOutcome open(Player viewer, Optional<NpcInfo> target, boolean checkAccess) {
        if (target.isEmpty()) {
            return OpenOutcome.NOT_FOUND;
        }
        if (!skins.isDressable(target.get().id())) {
            return OpenOutcome.NOT_A_PLAYER_NPC;
        }
        if (checkAccess && !access.canOpen(viewer, target.get())) {
            return OpenOutcome.NO_ACCESS;
        }
        return opener.open(viewer, WardrobeKind.NPC, target.get().id());
    }

    /** Whether {@code viewer} may open the wardrobe of {@code npc}: it is a player NPC and the access rules let them. */
    public boolean mayOpen(Player viewer, NpcInfo npc) {
        return skins.isDressable(npc.id()) && access.canOpen(viewer, npc);
    }

    /** Tells {@code viewer} why an NPC's wardrobe did not open; nothing if it did. */
    public void tell(Player viewer, OpenOutcome outcome) {
        opener.tell(viewer, WardrobeKind.NPC, outcome);
    }
}
