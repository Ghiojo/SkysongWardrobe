package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Minecraft gives every player their own skin back when they log in, so a join fires
 * {@link WardrobeUpdateEvent} to draw the outfit again. A quit drops the cached wardrobe,
 * unless staff still have it open.
 */
public final class WardrobeSessionListener implements Listener {
    private final WardrobeRepository wardrobes;
    private final Server server;

    public WardrobeSessionListener(WardrobeRepository wardrobes, Server server) {
        this.wardrobes = wardrobes;
        this.server = server;
    }

    /** Monitor, so skin plugins such as SkinsRestorer have set the base skin first. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        server.getPluginManager().callEvent(new WardrobeUpdateEvent(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID owner = event.getPlayer().getUniqueId();
        boolean stillOpen = server.getOnlinePlayers().stream()
                .filter(online -> !online.equals(event.getPlayer()))
                .map(Player::getOpenInventory)
                .anyMatch(view -> WardrobeMenu.of(view).filter(menu -> menu.wardrobe().owner().equals(owner)).isPresent());
        if (!stillOpen) {
            wardrobes.unload(owner);
        }
    }
}
