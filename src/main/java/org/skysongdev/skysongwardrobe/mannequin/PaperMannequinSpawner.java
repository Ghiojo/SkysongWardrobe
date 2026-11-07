package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.MainHand;
import org.skysongdev.skysongwardrobe.render.ArmModel;

/**
 * Spawns Paper's vanilla mannequin as furniture: it can't be pushed, shows no name or
 * description, never despawns, and is saved with its chunk. Damage is handled by the plugin's
 * break listener, not here. The arm model comes with its skin, which the renderer sets.
 */
public final class PaperMannequinSpawner implements MannequinSpawner {
    @Override
    public LivingEntity spawn(Location location, ArmModel model) {
        return location.getWorld().spawn(location, Mannequin.class, mannequin -> {
            mannequin.setImmovable(true);
            mannequin.setDescription(null);
            mannequin.setCustomNameVisible(false);
            mannequin.setMainHand(MainHand.RIGHT);
            mannequin.setRemoveWhenFarAway(false);
            mannequin.setPersistent(true);
            mannequin.setSilent(true);
            mannequin.setCollidable(false);
        });
    }
}
