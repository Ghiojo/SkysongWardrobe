package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.logging.Level;

/** Renames a joining player's gallery folder to their current name, off the main thread. */
public final class GalleryFolderListener implements Listener {
    private final Plugin plugin;
    private final GalleryFolders folders;

    public GalleryFolderListener(Plugin plugin, GalleryFolders folders) {
        this.plugin = plugin;
        this.folders = folders;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                folders.claim(player.getUniqueId(), player.getName());
            } catch (IOException failed) {
                plugin.getLogger().log(Level.WARNING, "Could not rename the gallery folder of " + player.getName() + ".", failed);
            }
        });
    }
}
