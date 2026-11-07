package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LegacySkinTest {
    @Test
    void modernSkinIsUnchanged() {
        BufferedImage modern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        assertSame(modern, LegacySkin.toModern(modern).orElseThrow());
    }

    @Test
    void legacySkinGetsMirroredLeftLimbs() {
        BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        legacy.setRGB(4, 20, 0xFF112233);   // right leg front, top left
        legacy.setRGB(44, 20, 0xFF445566);  // right arm front, top left

        BufferedImage modern = LegacySkin.toModern(legacy).orElseThrow();

        assertEquals(64, modern.getHeight());
        assertEquals(0xFF112233, modern.getRGB(4, 20));
        assertEquals(0xFF112233, modern.getRGB(23, 52), "left leg front, mirrored");
        assertEquals(0xFF445566, modern.getRGB(39, 52), "left arm front, mirrored");
    }

    @Test
    void otherSizesAreRefused() {
        assertEquals(Optional.empty(), LegacySkin.toModern(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)));
        assertEquals(Optional.empty(), LegacySkin.toModern(new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB)));
    }
}
