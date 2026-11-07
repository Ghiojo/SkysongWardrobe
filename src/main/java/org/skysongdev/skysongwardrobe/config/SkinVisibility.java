package org.skysongdev.skysongwardrobe.config;

import java.util.Locale;
import java.util.Optional;

/**
 * Who may find a skin the plugin uploads to MineSkin. Public is left out on purpose: players'
 * skins must never show up in MineSkin's gallery or search.
 */
public enum SkinVisibility {
    /** Kept out of MineSkin's gallery and search, but reachable by its link. Every plan allows it. */
    UNLISTED,
    /** Only the uploading account sees it. Not every MineSkin plan allows it. */
    PRIVATE;

    /** The visibility a config value names, ignoring case, if it names one. */
    public static Optional<SkinVisibility> named(String value) {
        String wanted = value.trim().toUpperCase(Locale.ROOT);
        for (SkinVisibility visibility : values()) {
            if (visibility.name().equals(wanted)) {
                return Optional.of(visibility);
            }
        }
        return Optional.empty();
    }
}
