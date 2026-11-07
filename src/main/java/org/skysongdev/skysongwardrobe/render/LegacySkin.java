package org.skysongdev.skysongwardrobe.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Turns a pre-1.8 64x32 skin into the 64x64 layout, the way the Minecraft client does: the
 * left arm and leg, which old skins lack, are mirrored copies of the right ones.
 */
public final class LegacySkin {
    private static final int LEGACY_HEIGHT = 32;
    /** x, y, offset x, offset y, width, height: Minecraft's own copy list, every one mirrored sideways. */
    private static final int[][] COPIES = {
            {4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4},
            {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12}, {8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12},
            {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4},
            {40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12},
    };

    private LegacySkin() {
    }

    /** The image as a 64x64 skin: unchanged if it already is one, upgraded if it is 64x32, else empty. */
    public static Optional<BufferedImage> toModern(BufferedImage image) {
        if (image.getWidth() != SkinComposer.SIZE) {
            return Optional.empty();
        }
        if (image.getHeight() == SkinComposer.SIZE) {
            return Optional.of(image);
        }
        if (image.getHeight() != LEGACY_HEIGHT) {
            return Optional.empty();
        }
        BufferedImage modern = new BufferedImage(SkinComposer.SIZE, SkinComposer.SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = modern.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        for (int[] copy : COPIES) {
            mirrorCopy(modern, copy[0], copy[1], copy[2], copy[3], copy[4], copy[5]);
        }
        return Optional.of(modern);
    }

    private static void mirrorCopy(BufferedImage image, int x, int y, int offsetX, int offsetY, int width, int height) {
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                int pixel = image.getRGB(x + column, y + row);
                image.setRGB(x + offsetX + width - 1 - column, y + offsetY + row, pixel);
            }
        }
    }
}
