package org.skysongdev.skysongwardrobe.api;

/** Whether opening a wardrobe checks that the viewer may open it. */
public enum Access {
    /**
     * Check the same rules as the commands: a player's own wardrobe needs
     * {@code skysongwardrobe.wardrobe} and someone else's the {@code .other} permission; an
     * NPC's needs its access rules (staff, its Citizens owner, or a rule another plugin
     * registered); a mannequin's needs its owner, staff, or the WorldGuard flags.
     */
    HONOR,
    /** Skip those rules: the calling plugin already decided the viewer may open it. */
    BYPASS
}
