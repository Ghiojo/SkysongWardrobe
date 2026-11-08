package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseSkinsTest {
    private static final UUID PLAYER = UUID.randomUUID();
    private static final SignedTexture MOJANG = texture("http://textures.minecraft.net/texture/steve", false);
    private static final SignedTexture SLIM = texture("http://textures.minecraft.net/texture/alex", true);
    private static final SignedTexture OURS = new SignedTexture("ours", "sig");

    @TempDir
    Path folder;
    private final List<URI> fetched = new ArrayList<>();
    private BaseSkins bases;

    @BeforeEach
    void setUp() throws IOException {
        TextureCache cache = YamlTextureCache.load(folder.resolve("cache.yml"));
        cache.put("hash", OURS);
        bases = new BaseSkins(cache, uri -> {
            fetched.add(uri);
            return Optional.of(new BufferedImage(64, uri.getPath().endsWith("old") ? 32 : 64, BufferedImage.TYPE_INT_ARGB));
        }, () -> false, Logger.getAnonymousLogger());
    }

    @Test
    void aSkinThatIsNotOursBecomesTheBase() {
        BaseSkin base = bases.resolve(PLAYER, Optional.of(MOJANG)).orElseThrow();

        assertEquals(MOJANG, base.texture());
        assertEquals(ArmModel.CLASSIC, base.armModel());
        assertEquals(List.of(URI.create("http://textures.minecraft.net/texture/steve")), fetched);
    }

    @Test
    void ourOwnSkinKeepsTheEarlierBase() {
        BaseSkin first = bases.resolve(PLAYER, Optional.of(SLIM)).orElseThrow();

        assertSame(first, bases.resolve(PLAYER, Optional.of(OURS)).orElseThrow());
        assertEquals(ArmModel.SLIM, first.armModel());
    }

    @Test
    void theSameBaseIsNotDownloadedTwice() {
        bases.resolve(PLAYER, Optional.of(MOJANG));
        bases.resolve(PLAYER, Optional.of(MOJANG));

        assertEquals(1, fetched.size());
    }

    @Test
    void aNewOutsideSkinReplacesTheBase() {
        bases.resolve(PLAYER, Optional.of(MOJANG));

        assertEquals(SLIM, bases.resolve(PLAYER, Optional.of(SLIM)).orElseThrow().texture());
    }

    @Test
    void legacyBaseIsUpgraded() {
        assertEquals(64, bases.resolve(PLAYER, Optional.of(texture("http://t/old", false))).orElseThrow().image().getHeight());
    }

    @Test
    void noTextureOrABrokenOneGivesNoBase() {
        assertTrue(bases.resolve(PLAYER, Optional.empty()).isEmpty());
        assertTrue(bases.resolve(PLAYER, Optional.of(new SignedTexture("garbage", "sig"))).isEmpty());
        assertTrue(bases.resolve(PLAYER, Optional.of(OURS)).isEmpty());
    }

    @Test
    void forgetDropsTheBase() {
        bases.resolve(PLAYER, Optional.of(MOJANG));
        bases.forget(PLAYER);

        assertTrue(bases.resolve(PLAYER, Optional.of(OURS)).isEmpty());
    }

    private static SignedTexture texture(String url, boolean slim) {
        String metadata = slim ? ",\"metadata\":{\"model\":\"slim\"}" : "";
        return new SignedTexture(TextureInfoTest.encode("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"" + metadata + "}}}"), "sig");
    }
}
