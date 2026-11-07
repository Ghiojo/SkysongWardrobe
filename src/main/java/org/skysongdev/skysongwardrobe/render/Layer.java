package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Color;

import java.awt.image.BufferedImage;

/**
 * One image to draw over the skin, with the colour it is tinted by. White leaves it unchanged,
 * which is what regular layers and undyed items use.
 */
public record Layer(BufferedImage image, Color tint) {
    public static Layer untinted(BufferedImage image) {
        return new Layer(image, Color.WHITE);
    }
}
