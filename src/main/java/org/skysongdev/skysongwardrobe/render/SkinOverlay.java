package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Where each body part's second layer (hat, jacket, sleeves, trousers) sits on a 64x64 skin,
 * relative to its first layer. Drawing over the first layer must also clear the second layer
 * right above it, or the skin underneath shows through the clothes.
 */
final class SkinOverlay {
    /** A first-layer rectangle and how far its second layer is moved from it. */
    private record Part(int x, int y, int width, int height, int overlayDx, int overlayDy) {
    }

    private static final List<Part> PARTS = List.of(
            new Part(0, 0, 32, 16, 32, 0),     // head -> hat
            new Part(0, 16, 16, 16, 0, 16),    // right leg -> right trouser
            new Part(16, 16, 24, 16, 0, 16),   // body -> jacket
            new Part(40, 16, 16, 16, 0, 16),   // right arm -> right sleeve
            new Part(16, 48, 16, 16, -16, 0),  // left leg -> left trouser
            new Part(32, 48, 16, 16, 16, 0));  // left arm -> left sleeve

    private SkinOverlay() {
    }

    /**
     * Makes {@code skin}'s second layer transparent above every first-layer pixel that
     * {@code layer} draws on, before the layer itself is drawn. Both images are 64x64.
     */
    static void clearUnder(BufferedImage skin, BufferedImage layer) {
        for (Part part : PARTS) {
            for (int y = part.y(); y < part.y() + part.height(); y++) {
                for (int x = part.x(); x < part.x() + part.width(); x++) {
                    if (layer.getRGB(x, y) >>> 24 != 0) {
                        skin.setRGB(x + part.overlayDx(), y + part.overlayDy(), 0);
                    }
                }
            }
        }
    }
}
