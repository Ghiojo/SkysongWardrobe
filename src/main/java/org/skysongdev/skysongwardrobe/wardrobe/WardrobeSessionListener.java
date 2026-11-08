package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minecraft gives every player their own skin back when they log in, so a join fires
 * {@link WardrobeUpdateEvent} to draw the outfit again. A quit drops the cached wardrobe,
 * unless staff still have it open.
 */
public final class WardrobeSessionListener implements Listener {
    private final WardrobeRepository wardrobes;
    private final Server server;

    private final Logger logger;

    public WardrobeSessionListener(WardrobeRepository wardrobes, Server server, Logger logger) {
        this.wardrobes = wardrobes;
        this.server = server;
        this.logger = logger;
    }

    /**
     * Reads the wardrobe while the player is still logging in, on Paper's login thread, so the
     * main thread finds it loaded and never waits on disk or the database. Monitor, so a login
     * another plugin refused is skipped.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            wardrobes.load(event.getUniqueId());
        } catch (StorageException failed) {
            logger.log(Level.SEVERE, "Could not read the wardrobe of " + event.getName() + " at login.", failed);
        }
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
