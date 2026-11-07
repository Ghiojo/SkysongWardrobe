package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongwardrobe.mannequin.MannequinSkins;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns config.yml, messages.yml, wardrobe.yml and the mannequin skin folder: writes the defaults on first
 * start, reads them all, and
 * swaps in new copies on {@link #reload()}. Other parts ask it for the current values, so
 * they see a reload without having to be rebuilt.
 */
public final class PluginConfiguration {
    static final String CONFIG_FILE = "config.yml";
    static final String MESSAGES_FILE = "messages.yml";
    static final String LAYOUT_FILE = "wardrobe.yml";
    static final String MANNEQUIN_SKINS_FOLDER = "mannequin-skins";

    private final Plugin plugin;
    private WardrobeSettings settings;
    private Messages messages;
    private WardrobeLayout layout;
    private GalleryMenuLayout galleryMenu;
    private MannequinSkins mannequinSkins;

    public PluginConfiguration(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Writes any missing default file, then reads all three and logs every value that fell back or failed. */
    public void reload() {
        saveDefault(CONFIG_FILE);
        saveDefault(MESSAGES_FILE);
        saveDefault(LAYOUT_FILE);

        SettingsParser parser = new SettingsParser();
        settings = parser.parse(YamlConfiguration.loadConfiguration(file(CONFIG_FILE)));
        parser.warnings().forEach(warning -> plugin.getLogger().warning(CONFIG_FILE + ": " + warning));

        messages = new Messages(YamlConfiguration.loadConfiguration(file(MESSAGES_FILE)),
                bundled(MESSAGES_FILE), plugin.getLogger());

        YamlConfiguration layoutFile = YamlConfiguration.loadConfiguration(file(LAYOUT_FILE));
        GalleryMenuParser galleryParser = new GalleryMenuParser();
        galleryMenu = galleryParser.parse(layoutFile);
        galleryParser.warnings().forEach(warning -> plugin.getLogger().warning(LAYOUT_FILE + ": " + warning));

        LayoutParser layoutParser = new LayoutParser();
        layout = layoutParser.parse(layoutFile).orElse(null);
        layoutParser.errors().forEach(error -> plugin.getLogger().severe(LAYOUT_FILE + ": " + error));
        if (layout == null) {
            plugin.getLogger().severe(LAYOUT_FILE + " has errors, so the wardrobe stays closed until it is fixed.");
        }

        mannequinSkins = MannequinSkins.load(file(MANNEQUIN_SKINS_FOLDER).toPath(), plugin.getLogger());
    }

    public WardrobeSettings settings() {
        return Objects.requireNonNull(settings, "reload() has not run yet");
    }

    public Messages messages() {
        return Objects.requireNonNull(messages, "reload() has not run yet");
    }

    /** The wardrobe menu layout, empty while wardrobe.yml has errors. */
    public Optional<WardrobeLayout> layout() {
        Objects.requireNonNull(settings, "reload() has not run yet");
        return Optional.ofNullable(layout);
    }

    /** The gallery menu's title and fixed items. */
    public GalleryMenuLayout galleryMenu() {
        return Objects.requireNonNull(galleryMenu, "reload() has not run yet");
    }

    /** The preset mannequin base skins. */
    public MannequinSkins mannequinSkins() {
        return Objects.requireNonNull(mannequinSkins, "reload() has not run yet");
    }

    private File file(String name) {
        return new File(plugin.getDataFolder(), name);
    }

    private void saveDefault(String name) {
        if (!file(name).exists()) {
            plugin.saveResource(name, false);
        }
    }

    private YamlConfiguration bundled(String name) {
        InputStream resource = plugin.getResource(name);
        if (resource == null) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
    }
}
