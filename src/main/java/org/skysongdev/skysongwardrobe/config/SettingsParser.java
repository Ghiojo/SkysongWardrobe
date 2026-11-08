package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns config.yml into {@link WardrobeSettings}. A missing or invalid value falls back to
 * its default and adds a warning, so one typo never stops the plugin from starting.
 */
public final class SettingsParser {
    static final String DEFAULT_GALLERY_PERMISSION = "skysongwardrobe.gallery";
    static final String DEFAULT_APPLY_PERMISSION = "skysongwardrobe.apply";
    static final int DEFAULT_MAX_DOWNLOAD_KB = 512;
    static final int DEFAULT_DOWNLOAD_TIMEOUT_SECONDS = 10;
    static final int DEFAULT_UPLOAD_COOLDOWN_SECONDS = 30;
    static final String DEFAULT_MANNEQUIN_PERMISSION = "skysongwardrobe.mannequin";
    static final int DEFAULT_MANNEQUINS_PER_PLAYER = 0;
    static final int DEFAULT_MANNEQUINS_PER_CHUNK = 15;
    static final String DEFAULT_NPC_PERMISSION = "skysongwardrobe.npc";
    static final SkinVisibility DEFAULT_SKIN_VISIBILITY = SkinVisibility.UNLISTED;

    private final List<String> warnings = new ArrayList<>();

    /** Reads the settings; {@link #warnings()} then lists every value that fell back to its default. */
    public WardrobeSettings parse(ConfigurationSection config) {
        warnings.clear();
        return new WardrobeSettings(
                config.getBoolean("verbose", false),
                permissions(config, "gallery.permissions", DEFAULT_GALLERY_PERMISSION),
                positiveInt(config, "gallery.max-download-kb", DEFAULT_MAX_DOWNLOAD_KB),
                positiveInt(config, "gallery.download-timeout-seconds", DEFAULT_DOWNLOAD_TIMEOUT_SECONDS),
                permissions(config, "apply.permissions", DEFAULT_APPLY_PERMISSION),
                nonNegativeInt(config, "render.upload-cooldown-seconds", DEFAULT_UPLOAD_COOLDOWN_SECONDS),
                config.getString("mineskin.api-key", "").trim(),
                visibility(config, "mineskin.visibility"),
                new MannequinSettings(
                        permissions(config, "mannequins.permissions", DEFAULT_MANNEQUIN_PERMISSION),
                        nonNegativeInt(config, "mannequins.max-per-player", DEFAULT_MANNEQUINS_PER_PLAYER),
                        nonNegativeInt(config, "mannequins.max-per-chunk", DEFAULT_MANNEQUINS_PER_CHUNK)),
                permissions(config, "npcs.permissions", DEFAULT_NPC_PERMISSION));
    }

    /** The values the last {@link #parse} replaced with their defaults, one line each. */
    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    private List<String> permissions(ConfigurationSection config, String path, String fallback) {
        if (!config.contains(path)) {
            return List.of(fallback);
        }
        List<String> values = config.getStringList(path).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        if (values.isEmpty()) {
            warnings.add(path + " lists no permissions, so it uses " + fallback + ".");
            return List.of(fallback);
        }
        return values;
    }

    private SkinVisibility visibility(ConfigurationSection config, String path) {
        if (!config.contains(path)) {
            return DEFAULT_SKIN_VISIBILITY;
        }
        Optional<SkinVisibility> named = SkinVisibility.named(String.valueOf(config.get(path)));
        if (named.isEmpty()) {
            warnings.add(path + " must be unlisted or private, so it uses unlisted.");
            return DEFAULT_SKIN_VISIBILITY;
        }
        return named.get();
    }

    private int positiveInt(ConfigurationSection config, String path, int fallback) {
        return intAtLeast(config, path, fallback, 1);
    }

    private int nonNegativeInt(ConfigurationSection config, String path, int fallback) {
        return intAtLeast(config, path, fallback, 0);
    }

    private int intAtLeast(ConfigurationSection config, String path, int fallback, int minimum) {
        if (!config.contains(path)) {
            return fallback;
        }
        if (!config.isInt(path) || config.getInt(path) < minimum) {
            warnings.add(path + " must be a whole number of at least " + minimum + ", so it uses " + fallback + ".");
            return fallback;
        }
        return config.getInt(path);
    }
}
