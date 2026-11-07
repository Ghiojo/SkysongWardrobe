package org.skysongdev.skysongwardrobe.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

/** What the regions at a location say about a mannequin action. */
public interface RegionRules {
    /** Whether the regions allow it; empty when no region covers the location. */
    Optional<Boolean> allows(Player player, Location location, MannequinAction action);
}
