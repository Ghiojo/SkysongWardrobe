package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;

/**
 * Runs a wardrobe menu's close action one tick after it closes: opening another menu while
 * the close event is still running would be undone by it.
 */
public final class CloseActions implements Listener {
    private final TaskRunner tasks;

    public CloseActions(TaskRunner tasks) {
        this.tasks = tasks;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof WardrobeMenu menu && event.getPlayer() instanceof Player viewer) {
            menu.closeAction().ifPresent(action -> tasks.sync(() -> action.accept(viewer)));
        }
    }
}
