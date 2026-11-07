package org.skysongdev.skysongwardrobe.stats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongstats.api.ItemSources;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeUpdateEvent;

/** SkysongStats can't see a wardrobe change, so this asks it to recount after each one. */
public final class StatsRecountListener implements Listener {
    private final ItemSources itemSources;

    public StatsRecountListener(ItemSources itemSources) {
        this.itemSources = itemSources;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWardrobeUpdate(WardrobeUpdateEvent event) {
        itemSources.recount(event.getPlayer());
    }
}
