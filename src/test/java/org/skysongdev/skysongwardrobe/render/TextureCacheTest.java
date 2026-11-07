package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureCacheTest {
    private static final SignedTexture SIGNED = new SignedTexture("value", "signature");

    @TempDir
    Path folder;

    @Test
    void storedTexturesSurviveAReload() throws IOException {
        Path file = folder.resolve("texture-cache.yml");
        TextureCache.load(file).put("abc", SIGNED);

        TextureCache reloaded = TextureCache.load(file);

        assertEquals(Optional.of(SIGNED), reloaded.find("abc"));
        assertTrue(reloaded.isOwn(SIGNED));
        assertFalse(reloaded.isOwn(new SignedTexture("mojang", "sig")));
    }

    @Test
    void missingFileIsAnEmptyCache() {
        assertEquals(Optional.empty(), TextureCache.load(folder.resolve("none.yml")).find("abc"));
    }

    @Test
    void sameImageSameHashAndOnePixelChangesIt() {
        BufferedImage first = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage second = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        assertEquals(TextureCache.hash(first), TextureCache.hash(second));
        assertEquals(64, TextureCache.hash(first).length());
        second.setRGB(63, 63, 0xFF000001);
        assertNotEquals(TextureCache.hash(first), TextureCache.hash(second));
    }
}
