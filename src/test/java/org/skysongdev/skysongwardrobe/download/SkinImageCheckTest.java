package org.skysongdev.skysongwardrobe.download;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinImageCheckTest {
    @Test
    void acceptsA64By64Png() {
        assertEquals(Optional.empty(), SkinImageCheck.check(PngFixtures.png(64, 64)));
    }

    @Test
    void refusesOtherSizes() {
        assertEquals(Optional.of(DownloadFailure.WRONG_SIZE), SkinImageCheck.check(PngFixtures.png(64, 32)));
        assertEquals(Optional.of(DownloadFailure.WRONG_SIZE), SkinImageCheck.check(PngFixtures.png(128, 128)));
    }

    @Test
    void refusesOtherFormatsAndBrokenFiles() {
        assertEquals(Optional.of(DownloadFailure.NOT_PNG), SkinImageCheck.check(PngFixtures.gif(64, 64)));
        assertEquals(Optional.of(DownloadFailure.NOT_PNG), SkinImageCheck.check(new byte[0]));
        byte[] truncated = Arrays.copyOf(PngFixtures.png(64, 64), 20);
        assertEquals(Optional.of(DownloadFailure.NOT_PNG), SkinImageCheck.check(truncated));
    }
}
