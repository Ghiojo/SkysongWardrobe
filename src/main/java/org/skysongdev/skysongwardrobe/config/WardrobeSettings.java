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
 * @param skinVisibility          who may find the skins uploaded to MineSkin
 * @param mannequins              who may place mannequins, and how many
 * @param npcPermissions          holding any one of these lets a player dress the Citizens NPCs
 *                                they own; the {@code .other} variant reaches every NPC
 */
public record WardrobeSettings(boolean verbose,
                               List<String> galleryPermissions,
                               int maxDownloadKb,
                               int downloadTimeoutSeconds,
                               List<String> applyPermissions,
                               int uploadCooldownSeconds,
                               String mineSkinApiKey,
                               SkinVisibility skinVisibility,
                               MannequinSettings mannequins,
                               List<String> npcPermissions) {

    public WardrobeSettings {
        galleryPermissions = List.copyOf(galleryPermissions);
        applyPermissions = List.copyOf(applyPermissions);
        Objects.requireNonNull(mineSkinApiKey, "mineSkinApiKey");
        Objects.requireNonNull(skinVisibility, "skinVisibility");
        Objects.requireNonNull(mannequins, "mannequins");
        npcPermissions = List.copyOf(npcPermissions);
    }

    /** Whether a MineSkin API key is set, which skin rendering needs. */
    public boolean hasMineSkinApiKey() {
        return !mineSkinApiKey.isBlank();
    }
}
