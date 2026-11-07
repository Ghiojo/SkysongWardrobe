package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.items.MainThreadItemTags;
import org.skysongdev.skysongwardrobe.render.ArmModel;
import org.skysongdev.skysongwardrobe.render.Layer;
import org.skysongdev.skysongwardrobe.render.LayerResolver;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderJob;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.SignResult;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TrackingTaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MannequinRendererTest {
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "outfit-sig");

    @TempDir
    Path folder;

    private SkysongWardrobe plugin;
    private final FakeMannequinBodies bodies = new FakeMannequinBodies();
    private final LayerResolver layers = mock(LayerResolver.class);
    private MannequinRepository mannequins;
    private WardrobeRepository wardrobes;
    private RenderQueue queue;
    private MannequinRenderer renderer;
    private Mannequin mannequin;
    private MainThreadItemTags tags;

    @BeforeEach
    void setUp() throws IOException {
        MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        Path skins = plugin.getDataFolder().toPath().resolve("mannequin-skins");
        Files.createDirectories(skins);
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", skins.resolve("oak.png").toFile());
        Files.writeString(new File(plugin.getDataFolder(), "config.yml").toPath(), "mineskin:\n  api-key: key\n");
        plugin.configuration().reload();

        mannequins = MannequinRepository.load(folder.resolve("mannequins"), Logger.getAnonymousLogger());
        wardrobes = new WardrobeRepository(folder.resolve("wardrobes"), Logger.getAnonymousLogger());
        TextureCache cache = TextureCache.load(folder.resolve("texture-cache.yml"));
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), (skin, arms) -> new SignResult.Signed(SIGNED), cache,
                () -> Duration.ZERO, Clock.systemUTC(), new RenderRouter(), Logger.getAnonymousLogger());
        TrackingTaskRunner tasks = new TrackingTaskRunner();
        tags = new MainThreadItemTags(tasks, new FakeItemTags());
        OutfitRenderer outfits = new OutfitRenderer(tags, layers, cache, queue, () -> true);
        renderer = new MannequinRenderer(mannequins, wardrobes, plugin.configuration(), outfits, bodies,
                tasks, Logger.getAnonymousLogger());
        when(layers.layers(anyList())).thenReturn(List.of());

        mannequin = new Mannequin(UUID.randomUUID(), UUID.randomUUID(), new ItemStack(Material.ARMOR_STAND), "oak",
                ArmModel.SLIM);
        mannequins.save(mannequin);
        bodies.loaded.add(mannequin.id());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aNewMannequinQueuesItsPlainSkinWithItsArmModel() {
        renderer.render(mannequin.id());

        RenderJob job = queue.waiting().getFirst();
        assertEquals(mannequin.id(), job.wearer());
        assertEquals(WearerKind.MANNEQUIN, job.kind());
        assertEquals(ArmModel.SLIM, job.armModel());
    }

    @Test
    void itemTagsAreReadOnTheMainThreadBeforeTheAsyncRender() {
        wardrobes.load(mannequin.id()).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));

        renderer.render(mannequin.id());

        assertEquals(1, tags.reads());
        assertTrue(queue.isWaiting(mannequin.id()));
    }

    @Test
    void aLookSignedBeforeShowsAtOnce() {
        renderer.render(mannequin.id());
        queue.runNext();

        renderer.render(mannequin.id());

        assertEquals(SIGNED, bodies.skins.get(mannequin.id()));
        assertFalse(queue.isWaiting(mannequin.id()));
    }

    @Test
    void wornLayersAreDrawn() {
        BufferedImage red = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        red.setRGB(8, 8, 0xFFFF0000);
        when(layers.layers(anyList())).thenReturn(List.of(Layer.untinted(red)));

        renderer.render(mannequin.id());

        assertEquals(0xFFFF0000, queue.waiting().getFirst().skin().getRGB(8, 8));
    }

    @Test
    void equipmentIsPutOnAtOnce() {
        Wardrobe wardrobe = wardrobes.load(mannequin.id());
        wardrobe.put("equipment:head", new ItemStack(Material.IRON_HELMET));
        wardrobe.put("equipment:main_hand", new ItemStack(Material.STICK));

        renderer.render(mannequin.id());

        Map<EquipmentSlot, ItemStack> worn = bodies.equipment.get(mannequin.id());
        assertEquals(Material.IRON_HELMET, worn.get(EquipmentSlot.HEAD).getType());
        assertEquals(Material.STICK, worn.get(EquipmentSlot.HAND).getType());
        assertEquals(2, worn.size());
    }

    @Test
    void aMissingSkinFileRendersNothing() throws IOException {
        Mannequin marble = new Mannequin(UUID.randomUUID(), UUID.randomUUID(), new ItemStack(Material.ARMOR_STAND),
                "marble", ArmModel.CLASSIC);
        mannequins.save(marble);

        renderer.render(marble.id());

        assertTrue(queue.waiting().isEmpty());
    }

    @Test
    void anUnknownMannequinIsIgnored() {
        renderer.render(UUID.randomUUID());

        assertTrue(queue.waiting().isEmpty());
    }
}
