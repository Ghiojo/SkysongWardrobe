package org.skysongdev.skysongwardrobe.config;

import java.util.List;

/**
 * The mannequin part of config.yml, already checked. Built by {@link SettingsParser}.
 *
 * @param permissions holding any one of these lets a player place mannequins, and use and break
 *                    their own; the {@code .other} variant reaches everyone's
 * @param perPlayer   the most mannequins one player may have placed, 0 for no limit
 * @param perChunk    the most mannequins one chunk may hold, 0 for no limit
 */
public record MannequinSettings(List<String> permissions, int perPlayer, int perChunk) {
    public MannequinSettings {
        permissions = List.copyOf(permissions);
    }

    /** Whether {@code placed} more mannequins would still be within a limit; 0 means none. */
    public static boolean withinLimit(int limit, int placed) {
        return limit == 0 || placed < limit;
    }
}
