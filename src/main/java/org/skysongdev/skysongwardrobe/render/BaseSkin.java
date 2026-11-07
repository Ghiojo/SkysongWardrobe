package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;

/** The skin a player has under their outfit, with its signed texture, arm model and 64x64 image. */
public record BaseSkin(SignedTexture texture, ArmModel armModel, BufferedImage image) {
}
