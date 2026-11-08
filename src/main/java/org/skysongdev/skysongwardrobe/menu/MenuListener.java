package org.skysongdev.skysongwardrobe.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Keeps read-only menus, such as the gallery, read-only and passes their clicks on. */
public final class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof ClickableMenu menu
                && event.getWhoClicked() instanceof Player viewer) {
            // Cancelled for clicks in the player's own inventory too, so shift-clicks can't move items in.
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                menu.click(event.getSlot(), viewer);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof ClickableMenu) {
            event.setCancelled(true);
        }
    }
}
