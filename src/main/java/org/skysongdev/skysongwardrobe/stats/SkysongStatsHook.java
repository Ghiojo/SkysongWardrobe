package org.skysongdev.skysongwardrobe.stats;

import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongstats.api.ItemSources;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

/**
 * Makes worn wardrobe items count towards SkysongStats. The only class that touches the
 * SkysongStats API at startup, so the plugin loads it only when SkysongStats is enabled.
 * SkysongStats removes the source itself when this plugin disables.
 */
public final class SkysongStatsHook {
    /** The source's name in SkysongStats. */
    public static final String SOURCE_ID = "wardrobe";

    private SkysongStatsHook() {
    }

    /** Registers the worn items as an item source and recounts after each wardrobe change. */
    public static void register(Plugin plugin, WardrobeRepository wardrobes, PluginConfiguration configuration) {
        register(plugin, SkysongStatsApi.get().itemSources(), wardrobes, configuration);
    }

    static void register(Plugin plugin, ItemSources itemSources, WardrobeRepository wardrobes,
                         PluginConfiguration configuration) {
        itemSources.register(plugin, SOURCE_ID, new WornItemSource(wardrobes, configuration::layout));
        plugin.getServer().getPluginManager().registerEvents(new StatsRecountListener(itemSources), plugin);
    }
}
