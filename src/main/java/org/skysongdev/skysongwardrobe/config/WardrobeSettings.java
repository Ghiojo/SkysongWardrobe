package org.skysongdev.skysongwardrobe.config;

import java.util.List;
import java.util.Objects;

/**
 * Everything config.yml sets, already checked. Built by {@link SettingsParser}.
 *
 * @param verbose                 whether silent failures, such as a missing skin file, are logged
 * @param galleryPermissions      holding any one of these grants the skin gallery
 * @param maxDownloadKb           the largest skin file an upload may download
 * @param downloadTimeoutSeconds  how long an upload may take to download
 * @param applyPermissions        holding any one of these grants /sw apply and /sw unapply
 * @param uploadCooldownSeconds   the shortest time between two MineSkin uploads for one player
 * @param mineSkinApiKey          the MineSkin API key, empty when none is set
 * @param mannequins              who may place mannequins, and how many
 */
public record WardrobeSettings(boolean verbose,
                               List<String> galleryPermissions,
                               int maxDownloadKb,
                               int downloadTimeoutSeconds,
                               List<String> applyPermissions,
                               int uploadCooldownSeconds,
                               String mineSkinApiKey,
                               MannequinSettings mannequins) {

    public WardrobeSettings {
        galleryPermissions = List.copyOf(galleryPermissions);
        applyPermissions = List.copyOf(applyPermissions);
        Objects.requireNonNull(mineSkinApiKey, "mineSkinApiKey");
        Objects.requireNonNull(mannequins, "mannequins");
    }

    /** Whether a MineSkin API key is set, which skin rendering needs. */
    public boolean hasMineSkinApiKey() {
        return !mineSkinApiKey.isBlank();
    }
}
