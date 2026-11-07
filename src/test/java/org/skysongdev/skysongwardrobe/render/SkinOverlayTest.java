package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinOverlayTest {
    private static final int SKIN = 0xFFFF0000;
    private static final int HAIR = 0xFF00FF00;
    private static final int SHIRT = 0xFF0000FF;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "head,           10, 10, 42, 10",
            "right leg,       4, 20,  4, 36",
            "body,           20, 20, 20, 36",
            "right arm,      44, 20, 44, 36",
            "left leg,       20, 52,  4, 52",
            "left arm,       36, 52, 52, 52"})
    void drawingOnAFirstLayerPixelClearsTheSecondLayerAboveIt(String part, int x, int y, int overlayX, int overlayY) {
        BufferedImage skin = filled(HAIR);
        BufferedImage layer = empty();
        layer.setRGB(x, y, SHIRT);

        SkinOverlay.clearUnder(skin, layer);

        assertEquals(0, skin.getRGB(overlayX, overlayY));
        assertEquals(HAIR, skin.getRGB(overlayX + 1, overlayY));
    }

    @Test
    void anEmptyLayerClearsNothing() {
        BufferedImage skin = filled(HAIR);

        SkinOverlay.clearUnder(skin, empty());

        assertEquals(HAIR, skin.getRGB(42, 10));
    }

    @Test
    void aBareSecondLayerHidesTheSkinsOwnSecondLayerWhenComposed() {
        BufferedImage base = empty();
        base.setRGB(20, 20, SKIN);
        base.setRGB(20, 36, HAIR);
        BufferedImage shirt = empty();
        shirt.setRGB(20, 20, SHIRT);

        BufferedImage result = SkinComposer.compose(base, List.of(Layer.untinted(shirt)));

        assertEquals(SHIRT, result.getRGB(20, 20));
        assertEquals(0, result.getRGB(20, 36));
    }

    @Test
    void aLayersOwnSecondLayerIsKept() {
        BufferedImage base = empty();
        base.setRGB(20, 36, HAIR);
        BufferedImage jacket = empty();
        jacket.setRGB(20, 20, SHIRT);
        jacket.setRGB(20, 36, SHIRT);

        BufferedImage result = SkinComposer.compose(base, List.of(Layer.untinted(jacket)));

        assertEquals(SHIRT, result.getRGB(20, 36));
    }

    @Test
    void aHigherLayerAlsoHidesALowerLayersSecondLayer() {
        BufferedImage lower = empty();
        lower.setRGB(20, 36, HAIR);
        BufferedImage upper = empty();
        upper.setRGB(20, 20, SHIRT);

        BufferedImage result = SkinComposer.compose(empty(), List.of(Layer.untinted(lower), Layer.untinted(upper)));

        assertEquals(0, result.getRGB(20, 36));
    }

    @Test
    void secondLayerPixelsOfALayerClearNothing() {
        BufferedImage base = empty();
        base.setRGB(52, 52, HAIR);
        BufferedImage sleeve = empty();
        sleeve.setRGB(36, 36, SHIRT);

        BufferedImage result = SkinComposer.compose(base, List.of(Layer.untinted(sleeve)));

        assertEquals(HAIR, result.getRGB(52, 52));
    }

    private static BufferedImage empty() {
        return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    }

    private static BufferedImage filled(int argb) {
        BufferedImage image = empty();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }
}
