package org.skysongdev.skysongwardrobe.render;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;

import java.util.Optional;

/** Reads and swaps the {@code textures} property of a player's profile. Main thread only. */
public final class PlayerSkins {
    private static final String TEXTURES = "textures";

    private PlayerSkins() {
    }

    /** The player's signed texture right now, or empty when they have none (such as offline mode). */
    public static Optional<SignedTexture> current(Player player) {
        return player.getPlayerProfile().getProperties().stream()
                .filter(property -> TEXTURES.equals(property.getName()) && property.isSigned())
                .findFirst()
                .map(property -> new SignedTexture(property.getValue(), property.getSignature()));
    }

    /** Puts the texture on the player; {@link Player#setPlayerProfile} shows it to everyone. */
    public static void apply(Player player, SignedTexture texture) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperty(TEXTURES);
        profile.setProperty(new ProfileProperty(TEXTURES, texture.value(), texture.signature()));
        player.setPlayerProfile(profile);
    }
}
