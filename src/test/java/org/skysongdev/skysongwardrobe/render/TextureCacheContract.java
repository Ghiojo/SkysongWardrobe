package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link TextureCache} must do, whatever it stores the skins in. Each implementation's
 * test extends this, so they all behave the same. Public, so the SQL caches' tests in other
 * packages extend it too.
 */
public abstract class TextureCacheContract {
    protected static final SignedTexture SIGNED = new SignedTexture("value", "signature");

    private TextureCache cache;

    /** A cache with nothing in it yet. */
    protected abstract TextureCache emptyCache() throws IOException;

    @BeforeEach
    public void createCache() throws IOException {
        cache = emptyCache();
    }

    @Test
    public void anEmptyCacheFindsNothing() {
        assertEquals(Optional.empty(), cache.find("abc"));
        assertFalse(cache.isOwn(SIGNED));
    }

    @Test
    public void aStoredTextureIsFoundByItsHash() throws IOException {
        cache.put("abc", SIGNED);

        assertEquals(Optional.of(SIGNED), cache.find("abc"));
        assertEquals(Optional.empty(), cache.find("def"));
    }

    @Test
    public void aStoredTextureIsOwnAndOthersAreNot() throws IOException {
        cache.put("abc", SIGNED);

        assertTrue(cache.isOwn(SIGNED));
        assertTrue(cache.isOwn(new SignedTexture("value", "another signature")));
        assertFalse(cache.isOwn(new SignedTexture("mojang", "sig")));
    }

    @Test
    public void storingAHashAgainKeepsTheNewestTexture() throws IOException {
        SignedTexture newer = new SignedTexture("newer", "newer signature");
        cache.put("abc", SIGNED);
        cache.put("abc", newer);

        assertEquals(Optional.of(newer), cache.find("abc"));
        assertTrue(cache.isOwn(newer));
    }

    @Test
    public void manyTexturesAreKeptApart() throws IOException {
        for (int i = 0; i < 50; i++) {
            cache.put("hash" + i, new SignedTexture("value" + i, "signature" + i));
        }

        assertEquals(Optional.of(new SignedTexture("value7", "signature7")), cache.find("hash7"));
        assertEquals(Optional.of(new SignedTexture("value49", "signature49")), cache.find("hash49"));
        assertTrue(cache.isOwn(new SignedTexture("value23", "signature23")));
    }

    @Test
    public void sameImageSameHashAndOnePixelChangesIt() {
        BufferedImage first = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage second = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        assertEquals(TextureCache.hash(first), TextureCache.hash(second));
        assertEquals(64, TextureCache.hash(first).length());
        second.setRGB(63, 63, 0xFF000001);
        assertNotEquals(TextureCache.hash(first), TextureCache.hash(second));
    }
}
