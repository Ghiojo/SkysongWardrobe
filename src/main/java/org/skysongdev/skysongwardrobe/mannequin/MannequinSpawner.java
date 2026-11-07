package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.skysongdev.skysongwardrobe.render.ArmModel;

/** Puts a mannequin entity in the world. Kept apart so the placing rules can be tested without one. */
public interface MannequinSpawner {
    /** Spawns a still, nameless mannequin at {@code location}, facing its yaw. Main thread only. */
    LivingEntity spawn(Location location, ArmModel model);
}
