package org.skysongdev.skysongwardrobe.render;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutfitRendererTest {
    private static final UUID WEARER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final SignedTexture BARE = new SignedTexture("bare", "bare-sig");
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "outfit-sig");
    private static final List<ItemStack> WORN = List.of();

    @TempDir
    Path folder;

    private final LayerResolver layers = mock(LayerResolver.class);
    private TextureCache cache;
    private RenderQueue queue;
    private boolean hasKey = true;
    private OutfitRenderer outfits;

    @BeforeEach
    void setUp() {
        cache = TextureCache.load(folder.resolve("texture-cache.yml"));
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), (skin, arms) -> new SignResult.Signed(SIGNED),
                cache, () -> Duration.ZERO, Clock.systemUTC(), new RenderRouter(), Logger.getLogger("test"));
        outfits = new OutfitRenderer(() -> layers, cache, queue, () -> hasKey);
        when(layers.layers(anyList())).thenReturn(List.of(Layer.untinted(solid(Color.RED))));
    }

    @Test
    void aNewLookIsQueuedWithItsKindAndArmModel() {
        OutfitRenderer.Outcome outcome = render(Optional.of(BARE));

        assertInstanceOf(OutfitRenderer.Outcome.Queued.class, outcome);
        RenderJob job = queue.waiting().getFirst();
        assertEquals(WearerKind.MANNEQUIN, job.kind());
        assertEquals(ArmModel.SLIM, job.armModel());
    }

    @Test
    void aCachedLookIsReadyAtOnce() throws IOException {
        cache.put(TextureCache.hash(SkinComposer.compose(solid(Color.BLUE), layers.layers(WORN))), SIGNED);

        assertEquals(new OutfitRenderer.Outcome.Ready(SIGNED), render(Optional.of(BARE)));
        assertFalse(queue.isWaiting(WEARER));
    }

    @Test
    void nothingWornShowsTheBareTexture() {
        when(layers.layers(anyList())).thenReturn(List.of());

        assertEquals(new OutfitRenderer.Outcome.Ready(BARE), render(Optional.of(BARE)));
    }

    @Test
    void nothingWornWithoutABareTextureSignsTheBaseAlone() {
        when(layers.layers(anyList())).thenReturn(List.of());

        assertInstanceOf(OutfitRenderer.Outcome.Queued.class, render(Optional.empty()));
        assertEquals(TextureCache.hash(solid(Color.BLUE)), queue.waiting().getFirst().hash());
    }

    @Test
    void aNewLookWithoutAnApiKeyIsNotQueued() {
        hasKey = false;

        assertInstanceOf(OutfitRenderer.Outcome.NoApiKey.class, render(Optional.of(BARE)));
        assertTrue(queue.waiting().isEmpty());
    }

    private OutfitRenderer.Outcome render(Optional<SignedTexture> bare) {
        return outfits.render(WEARER, WearerKind.MANNEQUIN, solid(Color.BLUE), ArmModel.SLIM, bare, WORN);
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }
}
