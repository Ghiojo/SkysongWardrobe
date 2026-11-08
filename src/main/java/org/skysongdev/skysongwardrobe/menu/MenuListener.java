package org.skysongdev.skysongwardrobe.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Keeps the gallery menu read-only and turns its pages. */
public final class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GalleryMenu menu) {
            // Cancelled for clicks in the player's own inventory too, so shift-clicks can't move items in.
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                menu.click(event.getSlot());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GalleryMenu) {
            event.setCancelled(true);
        }
    }
}
