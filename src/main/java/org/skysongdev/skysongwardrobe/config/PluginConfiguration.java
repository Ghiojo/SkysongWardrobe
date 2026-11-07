package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Owns config.yml and messages.yml: writes the defaults on first start, reads both, and
 * swaps in new copies on {@link #reload()}. Other parts ask it for the current values, so
 * they see a reload without having to be rebuilt.
 */
public final class PluginConfiguration {
    static final String CONFIG_FILE = "config.yml";
    static final String MESSAGES_FILE = "messages.yml";

    private final Plugin plugin;
    private WardrobeSettings settings;
    private Messages messages;

    public PluginConfiguration(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Writes any missing default file, then reads both files and logs every value that fell back. */
    public void reload() {
        saveDefault(CONFIG_FILE);
        saveDefault(MESSAGES_FILE);

        SettingsParser parser = new SettingsParser();
        settings = parser.parse(YamlConfiguration.loadConfiguration(file(CONFIG_FILE)));
        parser.warnings().forEach(warning -> plugin.getLogger().warning(CONFIG_FILE + ": " + warning));

        messages = new Messages(YamlConfiguration.loadConfiguration(file(MESSAGES_FILE)),
                bundled(MESSAGES_FILE), plugin.getLogger());
    }

    public WardrobeSettings settings() {
        return Objects.requireNonNull(settings, "reload() has not run yet");
    }

    public Messages messages() {
        return Objects.requireNonNull(messages, "reload() has not run yet");
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
