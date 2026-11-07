package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

/**
 * A composed skin waiting to be signed for a player.
 *
 * @param player   whose outfit it is
 * @param skin     the final 64x64 image
 * @param armModel the base skin's arm model, uploaded as the variant
 * @param hash     the image's {@link TextureCache#hash(BufferedImage)}
 */
public record RenderJob(UUID player, BufferedImage skin, ArmModel armModel, String hash) {
    public RenderJob {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skin, "skin");
        Objects.requireNonNull(armModel, "armModel");
        Objects.requireNonNull(hash, "hash");
    }

    /** A job for a freshly composed skin, hashing it. */
    public static RenderJob of(UUID player, BufferedImage skin, ArmModel armModel) {
        return new RenderJob(player, skin, armModel, TextureCache.hash(skin));
    }
}
