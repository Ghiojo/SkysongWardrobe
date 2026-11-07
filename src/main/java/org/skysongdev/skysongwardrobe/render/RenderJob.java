package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

/**
 * A composed skin waiting to be signed for a wearer.
 *
 * @param wearer   the player's or mannequin's UUID
 * @param kind     whether the wearer is a player or a mannequin
 * @param skin     the final 64x64 image
 * @param armModel the arm model uploaded as the variant
 * @param hash     the image's {@link TextureCache#hash(BufferedImage)}
 */
public record RenderJob(UUID wearer, WearerKind kind, BufferedImage skin, ArmModel armModel, String hash) {
    public RenderJob {
        Objects.requireNonNull(wearer, "wearer");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(skin, "skin");
        Objects.requireNonNull(armModel, "armModel");
        Objects.requireNonNull(hash, "hash");
    }

    /** A job for a freshly composed player skin, hashing it. */
    public static RenderJob of(UUID player, BufferedImage skin, ArmModel armModel) {
        return of(player, WearerKind.PLAYER, skin, armModel);
    }

    /** A job for a freshly composed skin, hashing it. */
    public static RenderJob of(UUID wearer, WearerKind kind, BufferedImage skin, ArmModel armModel) {
        return new RenderJob(wearer, kind, skin, armModel, TextureCache.hash(skin));
    }
}
