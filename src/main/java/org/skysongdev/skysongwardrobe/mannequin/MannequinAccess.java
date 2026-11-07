package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Who may place mannequins somewhere, and open or break a placed one. */
public interface MannequinAccess {
    boolean canPlace(Player player, Location location);

    boolean canUse(Player player, Mannequin mannequin, Location location);

    boolean canBreak(Player player, Mannequin mannequin, Location location);
}
