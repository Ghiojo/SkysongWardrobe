package org.skysongdev.skysongwardrobe;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point of SkysongWardrobe. It only builds and wires the plugin's parts;
 * each feature lives in its own package and is registered here as it is added.
 *
 * <p>Not final, because MockBukkit loads the plugin in tests through a generated subclass.
 */
public class SkysongWardrobe extends JavaPlugin {

    @Override
    public void onEnable() {
        // Features register here as they are added.
    }

    @Override
    public void onDisable() {
        // Nothing to release yet.
    }
}
