package org.skysongdev.skysongwardrobe;

import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.items.TagRegistrationListener;

/**
 * Entry point of SkysongWardrobe. It only builds and wires the plugin's parts;
 * each feature lives in its own package and is registered here as it is added.
 *
 * <p>Not final, because MockBukkit loads the plugin in tests through a generated subclass.
 */
public class SkysongWardrobe extends JavaPlugin {
    private PluginConfiguration configuration;

    @Override
    public void onEnable() {
        configuration = new PluginConfiguration(this);
        configuration.reload();
        if (!configuration.settings().hasMineSkinApiKey()) {
            getLogger().warning("No MineSkin API key in config.yml, so skins will not render.");
        }
        getServer().getPluginManager().registerEvents(new TagRegistrationListener(this, SkysongItemsApi::get), this);
    }

    @Override
    public void onDisable() {
        // Nothing to release yet.
    }

    /** The loaded config.yml and messages.yml. */
    public PluginConfiguration configuration() {
        return configuration;
    }
}
