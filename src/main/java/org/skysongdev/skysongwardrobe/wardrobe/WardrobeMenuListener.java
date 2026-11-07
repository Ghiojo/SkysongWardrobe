package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns inventory events in a wardrobe menu into {@link WardrobeMenu} moves. Clicks in the menu
 * are always cancelled and done by hand, so no item can slip past the slot rules. Shift-clicks
 * from the player's inventory go through the same rules; drags into the menu and number-key
 * swaps are refused. Every accepted change saves; closing a changed menu fires
 * {@link WardrobeUpdateEvent} for the owner, if they are online.
 */
public final class WardrobeMenuListener implements Listener {
    private final WardrobeRepository wardrobes;
    private final WardrobeRepository mannequinWardrobes;
    private final PluginConfiguration configuration;
    private final Server server;
    private final Logger logger;

    public WardrobeMenuListener(WardrobeRepository wardrobes, WardrobeRepository mannequinWardrobes,
                                PluginConfiguration configuration, Server server, Logger logger) {
        this.wardrobes = wardrobes;
        this.mannequinWardrobes = mannequinWardrobes;
        this.configuration = configuration;
        this.server = server;
        this.logger = logger;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof WardrobeMenu menu)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getInventory()) {
            onOwnInventoryClick(event, menu, player);
            return;
        }
        event.setCancelled(true);
        if (event.getClick().isKeyboardClick() || event.getAction() == InventoryAction.HOTBAR_SWAP) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (!cursor.isEmpty()) {
            WardrobeMenu.Move move = menu.placeInto(event.getSlot(), cursor);
            move.refusal().ifPresentOrElse(why -> refuse(player, why), () -> {
                event.getView().setCursor(move.rest());
                saved(menu, player);
            });
            return;
        }
        menu.takeOut(event.getSlot()).ifPresent(item -> {
            if (player.getInventory().addItem(item).isEmpty()) {
                saved(menu, player);
            } else {
                menu.undoTakeOut(event.getSlot(), item);
                player.sendMessage(configuration.messages().get("wardrobe.inventory-full"));
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof WardrobeMenu
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof WardrobeMenu menu) || !menu.changed()) {
            return;
        }
        if (menu.isMannequin()) {
            server.getPluginManager().callEvent(new MannequinUpdateEvent(menu.wardrobe().owner()));
        } else {
            Player owner = server.getPlayer(menu.wardrobe().owner());
            if (owner != null) {
                server.getPluginManager().callEvent(new WardrobeUpdateEvent(owner));
            }
        }
    }

    private void onOwnInventoryClick(InventoryClickEvent event, WardrobeMenu menu, Player player) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            // A double-click would gather matching items from the menu too.
            event.setCancelled(true);
        } else if (event.getClick().isShiftClick() && event.getCurrentItem() != null && !event.getCurrentItem().isEmpty()) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            WardrobeMenu.Move move = menu.placeAnywhere(item);
            move.refusal().ifPresentOrElse(why -> refuse(player, why), () -> {
                event.setCurrentItem(move.rest());
                saved(menu, player);
            });
        }
    }

    private void saved(WardrobeMenu menu, Player player) {
        try {
            (menu.isMannequin() ? mannequinWardrobes : wardrobes).save(menu.wardrobe());
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save the wardrobe of " + menu.wardrobe().owner() + ".", failed);
            player.sendMessage(configuration.messages().get("wardrobe.save-error"));
        }
        redrawOtherMenus(menu);
    }

    /** Staff and owner may have the same wardrobe open; keep both views current. */
    private void redrawOtherMenus(WardrobeMenu changed) {
        for (Player online : server.getOnlinePlayers()) {
            WardrobeMenu.of(online.getOpenInventory())
                    .filter(other -> other != changed && other.wardrobe() == changed.wardrobe())
                    .ifPresent(WardrobeMenu::draw);
        }
    }

    private void refuse(Player player, WardrobeMenu.Refusal why) {
        player.sendMessage(configuration.messages().get("wardrobe.refused." + why.name().toLowerCase(Locale.ROOT).replace('_', '-')));
    }
}
