package org.skysongdev.skysongwardrobe.download;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/** Checks that downloaded bytes are a 64x64 PNG, the only skin format the gallery takes. */
public final class SkinImageCheck {
    public static final int SKIN_SIZE = 64;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private SkinImageCheck() {
    }

    /** Empty if the bytes are a usable skin, or the reason they aren't. */
    public static Optional<DownloadFailure> check(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length
                || !Arrays.equals(bytes, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) {
            return Optional.of(DownloadFailure.NOT_PNG);
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException broken) {
            return Optional.of(DownloadFailure.NOT_PNG);
        }
        if (image == null) {
            return Optional.of(DownloadFailure.NOT_PNG);
        }
        if (image.getWidth() != SKIN_SIZE || image.getHeight() != SKIN_SIZE) {
            return Optional.of(DownloadFailure.WRONG_SIZE);
        }
        return Optional.empty();
    }
}
