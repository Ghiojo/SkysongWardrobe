package org.skysongdev.skysongwardrobe.render;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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
import org.skysongdev.skysongwardrobe.scheduling.ImmediateTaskRunner;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkinRendererTest {
    private static final SignedTexture MOJANG = new SignedTexture(TextureInfoTest.encode(
            "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/steve\"}}}"), "mojang-sig");
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "outfit-sig");

    @TempDir
    Path folder;

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock player;
    private final LayerResolver layers = mock(LayerResolver.class);
    private TextureCache cache;
    private RenderQueue queue;
    private SkinRenderer renderer;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        setApiKey("key");
        player = server.addPlayer();
        wear(MOJANG);
        plugin.wardrobes().load(player.getUniqueId()).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));

        cache = TextureCache.load(folder.resolve("texture-cache.yml"));
        ImmediateTaskRunner tasks = new ImmediateTaskRunner();
        SkinApplier applier = new SkinApplier(server, tasks);
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), (skin, arms) -> new SignResult.Signed(SIGNED), cache,
                () -> Duration.ZERO, Clock.systemUTC(), applier, Logger.getLogger("test"));
        BaseSkins bases = new BaseSkins(cache, uri -> Optional.of(solid(Color.BLUE)), () -> false, Logger.getLogger("test"));
        OutfitRenderer outfits = new OutfitRenderer(() -> layers, cache, queue,
                () -> plugin.configuration().settings().hasMineSkinApiKey());
        renderer = new SkinRenderer(plugin.wardrobes(), plugin.configuration(), bases, outfits, applier,
                tasks, server, Logger.getLogger("test"));
        when(layers.layers(anyList())).thenReturn(List.of(Layer.untinted(solid(Color.RED))));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aNewOutfitIsQueuedAndAppliedOnceSigned() {
        renderer.render(player);

        assertTrue(queue.isWaiting(player.getUniqueId()));
        assertEquals("[Wardrobe] Your new look is being prepared and will appear shortly.", nextMessage());

        queue.runNext();

        assertEquals(Optional.of(SIGNED), PlayerSkins.current(player));
    }

    @Test
    void anOutfitSignedBeforeIsAppliedWithoutQueueing() throws IOException {
        cache.put(hashOfOutfit(), SIGNED);

        renderer.render(player);

        assertFalse(queue.isWaiting(player.getUniqueId()));
        assertEquals(Optional.of(SIGNED), PlayerSkins.current(player));
    }

    @Test
    void anEmptyOutfitPutsTheBaseSkinBack() {
        renderer.render(player);
        queue.runNext();
        when(layers.layers(anyList())).thenReturn(List.of());
        player.nextMessage();

        renderer.render(player);

        assertEquals(Optional.of(MOJANG), PlayerSkins.current(player));
        assertNull(player.nextMessage());
    }

    @Test
    void nothingIsQueuedWithoutAnApiKey() throws IOException {
        setApiKey("");

        renderer.render(player);

        assertFalse(queue.isWaiting(player.getUniqueId()));
        assertEquals(Optional.of(MOJANG), PlayerSkins.current(player));
    }

    @Test
    void aPlayerWithoutASkinIsLeftAlone() {
        PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperty("textures");
        player.setPlayerProfile(profile);

        renderer.render(player);

        assertFalse(queue.isWaiting(player.getUniqueId()));
        assertEquals(Optional.empty(), PlayerSkins.current(player));
    }

    @Test
    void aSkinSignedAfterThePlayerLeftWaitsInTheCacheForTheirReturn() {
        renderer.render(player);
        player.disconnect();

        queue.runNext();

        assertTrue(cache.find(hashOfOutfit()).isPresent());
        assertFalse(queue.isWaiting(player.getUniqueId()));
    }

    @Test
    void appliesReplaceTheTexturesProperty() {
        PlayerSkins.apply(player, SIGNED);

        assertEquals(1, player.getPlayerProfile().getProperties().stream()
                .filter(property -> property.getName().equals("textures")).count());
        assertEquals(Optional.of(SIGNED), PlayerSkins.current(player));
    }

    private String hashOfOutfit() {
        return TextureCache.hash(SkinComposer.compose(solid(Color.BLUE), List.of(Layer.untinted(solid(Color.RED)))));
    }

    private void wear(SignedTexture texture) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.setProperty(new ProfileProperty("textures", texture.value(), texture.signature()));
        player.setPlayerProfile(profile);
    }

    private void setApiKey(String key) throws IOException {
        File config = new File(plugin.getDataFolder(), "config.yml");
        Files.writeString(config.toPath(), "mineskin:\n  api-key: \"" + key + "\"\n");
        plugin.configuration().reload();
    }

    private String nextMessage() {
        return PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
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
