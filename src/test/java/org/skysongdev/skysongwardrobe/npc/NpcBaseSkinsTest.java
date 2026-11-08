package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.render.ArmModel;
import org.skysongdev.skysongwardrobe.render.BaseSkin;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcBaseSkinsTest {
    private static final UUID NPC = UUID.randomUUID();
    private static final SignedTexture STEVE = texture("http://textures.minecraft.net/texture/steve", false);
    private static final SignedTexture ALEX = texture("http://textures.minecraft.net/texture/alex", true);
    private static final SignedTexture OURS = new SignedTexture("ours", "sig");

    @TempDir
    Path folder;
    private final List<URI> fetched = new ArrayList<>();
    private TextureCache cache;
    private NpcRepository records;
    private NpcBaseSkins bases;

    @BeforeEach
    void setUp() throws IOException {
        cache = TextureCache.load(folder.resolve("cache.yml"));
        cache.put("hash", OURS);
        records = new NpcRepository(folder.resolve("npcs"), Logger.getAnonymousLogger());
        bases = create();
    }

    private NpcBaseSkins create() {
        BaseSkins images = new BaseSkins(cache, uri -> {
            fetched.add(uri);
            return Optional.of(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
        }, () -> false, Logger.getAnonymousLogger());
        return new NpcBaseSkins(records, cache, images, Logger.getAnonymousLogger());
    }

    private static SignedTexture texture(String url, boolean slim) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\""
                + (slim ? ",\"metadata\":{\"model\":\"slim\"}" : "") + "}}}";
        return new SignedTexture(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)), "signed");
    }

    @Test
    void theNpcsOwnSkinBecomesTheSavedBase() {
        BaseSkin base = bases.resolve(NPC, Optional.of(ALEX)).orElseThrow();

        assertEquals(ALEX, base.texture());
        assertEquals(ArmModel.SLIM, base.armModel());
        assertEquals(Optional.of(ALEX), records.find(NPC).map(NpcRecord::base));
    }

    @Test
    void afterARestartOurRenderedSkinStillDrawsOnTheSavedBase() {
        bases.resolve(NPC, Optional.of(STEVE));

        BaseSkin base = create().resolve(NPC, Optional.of(OURS)).orElseThrow();

        assertEquals(STEVE, base.texture());
        assertEquals(Optional.of(STEVE), bases.saved(NPC));
    }

    @Test
    void aNewCitizensSkinReplacesTheSavedBase() {
        bases.resolve(NPC, Optional.of(STEVE));

        assertEquals(ALEX, bases.resolve(NPC, Optional.of(ALEX)).orElseThrow().texture());
        assertEquals(Optional.of(ALEX), new NpcRepository(folder.resolve("npcs"), Logger.getAnonymousLogger())
                .find(NPC).map(NpcRecord::base));
    }

    @Test
    void anNpcWithNoSkinOfItsOwnHasNoBase() {
        assertEquals(Optional.empty(), bases.resolve(NPC, Optional.empty()));
        assertEquals(Optional.empty(), bases.resolve(NPC, Optional.of(OURS)));
        assertEquals(Optional.empty(), records.find(NPC));
    }

    @Test
    void theSameBaseIsDownloadedOnce() {
        bases.resolve(NPC, Optional.of(STEVE));
        bases.resolve(NPC, Optional.of(OURS));
        bases.resolve(NPC, Optional.of(STEVE));

        assertEquals(1, fetched.size());
    }
}
