package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinComposerTest {
    private static final int OPAQUE_RED = 0xFFFF0000;
    private static final int OPAQUE_BLUE = 0xFF0000FF;

    @Test
    void tintMultipliesEachChannelAndKeepsAlpha() {
        BufferedImage grey = filled(0x80C08040);

        int tinted = SkinComposer.tint(grey, Color.fromRGB(0x80, 0xFF, 0x00)).getRGB(0, 0);

        assertEquals(0x80, tinted >>> 24);
        assertEquals(0xC0 * 0x80 / 255, (tinted >> 16) & 0xFF);
        assertEquals(0x80, (tinted >> 8) & 0xFF);
        assertEquals(0, tinted & 0xFF);
    }

    @Test
    void whiteTintChangesNothing() {
        BufferedImage image = filled(0xFF336699);

        assertEquals(0xFF336699, SkinComposer.tint(image, Color.WHITE).getRGB(5, 5));
    }

    @Test
    void layersCoverTheBaseWhereTheyAreOpaque() {
        BufferedImage layer = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        layer.setRGB(1, 1, OPAQUE_BLUE);

        BufferedImage result = SkinComposer.compose(filled(OPAQUE_RED), List.of(Layer.untinted(layer)));

        assertEquals(OPAQUE_BLUE, result.getRGB(1, 1));
        assertEquals(OPAQUE_RED, result.getRGB(0, 0));
    }

    @Test
    void halfTransparentLayersBlend() {
        int blended = SkinComposer.compose(filled(OPAQUE_RED), List.of(Layer.untinted(filled(0x800000FF)))).getRGB(0, 0);

        assertEquals(0xFF, blended >>> 24);
        assertEquals(0x7F, (blended >> 16) & 0xFF, 1);
        assertEquals(0x80, blended & 0xFF, 1);
    }

    @Test
    void laterLayersAreDrawnOverEarlierOnes() {
        BufferedImage result = SkinComposer.compose(filled(OPAQUE_RED),
                List.of(Layer.untinted(filled(0xFF00FF00)), Layer.untinted(filled(OPAQUE_BLUE))));

        assertEquals(OPAQUE_BLUE, result.getRGB(10, 10));
    }

    @Test
    void transparentPartsOfTheBaseStayTransparent() {
        BufferedImage base = filled(OPAQUE_RED);
        base.setRGB(40, 0, 0);

        assertEquals(0, SkinComposer.compose(base, List.of()).getRGB(40, 0) >>> 24);
    }

    private static BufferedImage filled(int argb) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }
}
