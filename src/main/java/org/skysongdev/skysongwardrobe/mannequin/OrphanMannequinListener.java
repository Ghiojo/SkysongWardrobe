package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.logging.Logger;

/**
 * Removes a marked mannequin whose file is gone when its chunk loads, such as one restored from
 * a world backup after it was broken. Its spawner was already given back, so keeping it would
 * duplicate the item. A mannequin whose file exists but can't be read is left alone.
 */
public final class OrphanMannequinListener implements Listener {
    private final MannequinRepository mannequins;
    private final MannequinMarker marker;
    private final Logger logger;

    public OrphanMannequinListener(MannequinRepository mannequins, MannequinMarker marker, Logger logger) {
        this.mannequins = mannequins;
        this.marker = marker;
        this.logger = logger;
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (marker.isMarked(entity) && !mannequins.isKnown(entity.getUniqueId())) {
                logger.warning("Removing mannequin " + entity.getUniqueId() + " at " + entity.getLocation().toVector()
                        + " in " + entity.getWorld().getName() + ": it has no file, so it was already broken.");
                entity.remove();
            }
        }
    }
}
