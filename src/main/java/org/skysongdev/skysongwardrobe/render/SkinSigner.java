package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;

/** Turns a rendered skin into a texture the Minecraft client accepts. Blocks, so call it off the main thread. */
@FunctionalInterface
public interface SkinSigner {
    /** Signs a 64x64 skin for the given arm model. Never throws; every failure is a {@link SignResult}. */
    SignResult sign(BufferedImage skin, ArmModel armModel);
}
