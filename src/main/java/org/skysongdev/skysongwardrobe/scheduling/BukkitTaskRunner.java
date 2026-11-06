package org.skysongdev.skysongwardrobe.scheduling;

import org.bukkit.plugin.Plugin;

/** {@link TaskRunner} on the Bukkit scheduler. */
public final class BukkitTaskRunner implements TaskRunner {
    private final Plugin plugin;

    public BukkitTaskRunner(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void async(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void sync(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
