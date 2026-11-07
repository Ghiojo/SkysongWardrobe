package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Player;

/** Who may open a mannequin's wardrobe and break it. */
public interface MannequinAccess {
    boolean canUse(Player player, Mannequin mannequin);

    boolean canBreak(Player player, Mannequin mannequin);
}
