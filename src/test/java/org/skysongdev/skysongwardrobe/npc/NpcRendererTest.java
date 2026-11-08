package org.skysongdev.skysongwardrobe.npc;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.items.MainThreadItemTags;
import org.skysongdev.skysongwardrobe.render.ArmModel;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.Layer;
import org.skysongdev.skysongwardrobe.render.LayerResolver;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderJob;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.SignResult;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TrackingTaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcRendererTest {
    private static final UUID NPC = UUID.randomUUID();
    private static final SignedTexture ALEX = new SignedTexture(Base64.getEncoder().encodeToString(
            "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/alex\",\"metadata\":{\"model\":\"slim\"}}}}"
                    .getBytes(StandardCharsets.UTF_8)), "alex-sig");
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "outfit-sig");

    @TempDir
    Path folder;

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock editor;
    private final FakeNpcSkins skins = new FakeNpcSkins();
    private final LayerResolver layers = mock(LayerResolver.class);
    private WardrobeRepository wardrobes;
    private RenderQueue queue;
    private NpcRenderer renderer;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        Files.writeString(new File(plugin.getDataFolder(), "config.yml").toPath(), "mineskin:\n  api-key: key\n");
        plugin.configuration().reload();
        editor = server.addPlayer();

        TextureCache cache = TextureCache.load(folder.resolve("texture-cache.yml"));
        TrackingTaskRunner tasks = new TrackingTaskRunner();
        NpcSkinApplier applier = new NpcSkinApplier(skins, tasks);
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), (skin, arms) -> new SignResult.Signed(SIGNED), cache,
                () -> Duration.ZERO, Clock.systemUTC(), applier, Logger.getAnonymousLogger());
        BaseSkins images = new BaseSkins(cache, uri -> Optional.of(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)),
                () -> false, Logger.getAnonymousLogger());
        NpcBaseSkins bases = new NpcBaseSkins(new NpcRepository(folder.resolve("npcs"), Logger.getAnonymousLogger()),
                cache, images, Logger.getAnonymousLogger());
        OutfitRenderer outfits = new OutfitRenderer(new MainThreadItemTags(tasks, new FakeItemTags()), layers, cache,
                queue, () -> true);
        wardrobes = new WardrobeRepository(folder.resolve("wardrobes"), Logger.getAnonymousLogger());
        renderer = new NpcRenderer(wardrobes, bases, skins, applier, outfits, plugin.configuration(), tasks, server,
                Logger.getAnonymousLogger());

        skins.dressable.add(NPC);
        skins.skins.put(NPC, ALEX);
        wardrobes.load(NPC).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        when(layers.layers(anyList())).thenReturn(List.of(Layer.untinted(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB))));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aNewOutfitIsQueuedAsAnNpcAndAppliedOnceSigned() {
        renderer.render(NPC, editor.getUniqueId());

        RenderJob job = queue.waiting().getFirst();
        assertEquals(WearerKind.NPC, job.kind());
        assertEquals(ArmModel.SLIM, job.armModel());
        assertEquals("[Wardrobe] The NPC's new look is being prepared and will appear shortly.", nextActionBar());

        queue.runNext();

        assertEquals(Optional.of(SIGNED), skins.current(NPC));
    }

    @Test
    void anEmptyWardrobePutsTheBaseSkinBackWithoutQueueing() {
        renderer.render(NPC, editor.getUniqueId());
        queue.runNext();
        wardrobes.load(NPC).put("coat", null);
        when(layers.layers(anyList())).thenReturn(List.of());
        editor.nextActionBar();

        renderer.render(NPC, editor.getUniqueId());

        assertEquals(Optional.of(ALEX), skins.current(NPC));
        assertNull(editor.nextActionBar());
    }

    @Test
    void anNpcWithoutASkinTellsTheEditor() {
        skins.skins.remove(NPC);

        renderer.render(NPC, editor.getUniqueId());

        assertTrue(queue.waiting().isEmpty());
        assertEquals("[Wardrobe] This NPC has no skin of its own yet. Give it one with /npc skin, then try again.",
                nextActionBar());
    }

    @Test
    void anNpcThatIsNotAPlayerIsLeftAlone() {
        skins.dressable.remove(NPC);

        renderer.render(NPC, editor.getUniqueId());

        assertTrue(queue.waiting().isEmpty());
        assertNull(editor.nextActionBar());
    }

    @Test
    void aSkinSignedAfterTheNpcWasRemovedIsDropped() {
        renderer.render(NPC, editor.getUniqueId());
        skins.dressable.remove(NPC);

        queue.runNext();

        assertFalse(skins.applied.contains(NPC));
    }

    @Test
    void aSkinTheNpcAlreadyWearsIsNotAppliedAgain() {
        NpcSkinApplier applier = new NpcSkinApplier(skins, new TrackingTaskRunner());

        applier.apply(NPC, ALEX);

        assertTrue(skins.applied.isEmpty());
    }

    private String nextActionBar() {
        return PlainTextComponentSerializer.plainText().serialize(editor.nextActionBar());
    }
}
