package org.skysongdev.skysongwardrobe.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

/**
 * A read-only menu: {@link MenuListener} cancels every click and drag in it and hands clicks on
 * its own slots here.
 */
public interface ClickableMenu extends InventoryHolder {
    /** {@code viewer} clicked the menu's slot {@code slot}. */
    void click(int slot, Player viewer);
}
