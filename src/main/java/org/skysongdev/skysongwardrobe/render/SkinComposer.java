package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Color;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Draws layers over a base skin into one 64x64 image. Layers are tinted the way Minecraft tints
 * a custom_model_data colour (each of red, green and blue multiplied by the colour's, divided by
 * 255, alpha kept), then alpha-blended over what is below, first layer lowest.
 */
public final class SkinComposer {
    public static final int SIZE = 64;

    private SkinComposer() {
    }

    public static BufferedImage compose(BufferedImage base, List<Layer> layers) {
        BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(base, 0, 0, SIZE, SIZE, null);
            graphics.setComposite(AlphaComposite.SrcOver);
            for (Layer layer : layers) {
                graphics.drawImage(tint(layer.image(), layer.tint()), 0, 0, SIZE, SIZE, null);
            }
        } finally {
            graphics.dispose();
        }
        return result;
    }

    /** A copy of the image with every pixel's colour multiplied by {@code color}. */
    public static BufferedImage tint(BufferedImage image, Color color) {
        BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = ((argb >> 16) & 0xFF) * color.getRed() / 255;
                int green = ((argb >> 8) & 0xFF) * color.getGreen() / 255;
                int blue = (argb & 0xFF) * color.getBlue() / 255;
                tinted.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
        return tinted;
    }
}
