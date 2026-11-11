package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Optional;

/**
 * Opens a placed mannequin's wardrobe, for whoever {@link MannequinAccess} allows where it
 * stands unless the caller already decided. Main thread only.
 */
public final class MannequinOpener {
    private final MannequinRepository mannequins;
    private final MannequinAccess access;
    private final WardrobeOpener opener;

    public MannequinOpener(MannequinRepository mannequins, MannequinAccess access, WardrobeOpener opener) {
        this.mannequins = mannequins;
        this.access = access;
        this.opener = opener;
    }

    /**
     * Shows {@code viewer} the wardrobe of the mannequin whose entity is {@code body}.
     *
     * @param checkAccess false when the caller already decided the viewer may open it
     */
    public OpenOutcome open(Player viewer, Entity body, boolean checkAccess, OpenSource source) {
        Optional<Mannequin> mannequin = mannequins.find(body.getUniqueId());
        if (mannequin.isEmpty()) {
            return OpenOutcome.NOT_FOUND;
        }
        if (checkAccess && !access.canUse(viewer, mannequin.get(), body.getLocation())) {
            return OpenOutcome.NO_ACCESS;
        }
        return opener.open(viewer, WardrobeKind.MANNEQUIN, mannequin.get().id(), source);
    }

    /** Tells {@code viewer} why a mannequin's wardrobe did not open; nothing if it did. */
    public void tell(Player viewer, OpenOutcome outcome) {
        opener.tell(viewer, WardrobeKind.MANNEQUIN, outcome);
    }
}
