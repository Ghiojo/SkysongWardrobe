package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Where each body part's second layer (hat, jacket, sleeves, trousers) sits on a 64x64 skin,
 * relative to its first layer. Drawing over the first layer must also deal with the second layer
 * right above it, or the skin underneath sits on top of the clothes.
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
     * Before {@code layer} is drawn, moves {@code skin}'s second layer down onto its first layer
     * under every first-layer pixel the layer draws on, and leaves the second layer transparent
     * there. Opaque clothing then covers it; see-through clothing shows it flattened beneath.
     * Both images are 64x64.
     */
    static void foldUnder(BufferedImage skin, BufferedImage layer) {
        for (Part part : PARTS) {
            for (int y = part.y(); y < part.y() + part.height(); y++) {
                for (int x = part.x(); x < part.x() + part.width(); x++) {
                    if (layer.getRGB(x, y) >>> 24 != 0) {
                        int overlayX = x + part.overlayDx();
                        int overlayY = y + part.overlayDy();
                        skin.setRGB(x, y, over(skin.getRGB(overlayX, overlayY), skin.getRGB(x, y)));
                        skin.setRGB(overlayX, overlayY, 0);
                    }
                }
            }
        }
    }

    /** {@code top} alpha-blended over {@code bottom}, both non-premultiplied ARGB. */
    static int over(int top, int bottom) {
        int topAlpha = top >>> 24;
        if (topAlpha == 0xFF) {
            return top;
        }
        if (topAlpha == 0) {
            return bottom;
        }
        int bottomAlpha = (bottom >>> 24) * (0xFF - topAlpha) / 0xFF;
        int alpha = topAlpha + bottomAlpha;
        int argb = alpha << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = (((top >> shift) & 0xFF) * topAlpha + ((bottom >> shift) & 0xFF) * bottomAlpha) / alpha;
            argb |= channel << shift;
        }
        return argb;
    }
}
