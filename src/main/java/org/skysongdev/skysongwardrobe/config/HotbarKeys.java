package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Which messages show above the hotbar instead of in chat, from the {@code hotbar} list in
 * messages.yml. Each entry is a message key or a whole section: {@code wardrobe} covers every
 * key under {@code wardrobe.}. A file without the list uses the bundled one.
 */
public final class HotbarKeys {
    private static final String LIST_KEY = "hotbar";

    private final List<String> entries;

    public HotbarKeys(List<String> entries) {
        this.entries = List.copyOf(entries);
    }

    /** The file's list, or the bundled one when the file has none. */
    public static HotbarKeys from(ConfigurationSection file, ConfigurationSection bundled) {
        ConfigurationSection source = file.isList(LIST_KEY) ? file : bundled;
        return new HotbarKeys(source.getStringList(LIST_KEY));
    }

    /** Whether the message at {@code key} goes above the hotbar. */
    public boolean contains(String key) {
        return entries.stream().anyMatch(entry -> key.equals(entry) || key.startsWith(entry + "."));
    }
}
