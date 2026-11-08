package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.render.LayerResolver;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.SignResult;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TrackingTaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class NpcWardrobesTest {
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "sig");

    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private final FakeNpcSkins skins = new FakeNpcSkins();
    private final RenderRouter router = new RenderRouter();
    private WardrobeKinds kinds;
    private NpcWardrobes npcs;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        TextureCache cache = YamlTextureCache.load(folder.resolve("cache.yml"));
        RenderQueue queue = RenderQueue.load(folder.resolve("queue.yml"), (skin, arms) -> new SignResult.Signed(SIGNED),
                cache, () -> Duration.ZERO, Clock.systemUTC(), router, Logger.getAnonymousLogger());
        TrackingTaskRunner tasks = new TrackingTaskRunner();
        OutfitRenderer outfits = new OutfitRenderer(FakeItemTags::new, mock(LayerResolver.class), cache, queue, () -> true);
        kinds = new WardrobeKinds();
        npcs = NpcWardrobes.start(plugin, folder.resolve("npcs"), skins, new NpcWardrobes.Shared(plugin.configuration(),
                kinds, router, queue, outfits, cache, uri -> Optional.empty(), tasks));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void npcWardrobesAreStoredAsTheirOwnKind() {
        assertSame(npcs.wardrobes(), kinds.get(WardrobeKind.NPC).orElseThrow().wardrobes());
    }

    @Test
    void closingAChangedNpcWardrobeAsksForItsRender() {
        UUID npc = UUID.randomUUID();
        PlayerMock editor = server.addPlayer();

        kinds.get(WardrobeKind.NPC).orElseThrow().changed().accept(npc, editor);

        server.getPluginManager().assertEventFired(NpcUpdateEvent.class,
                event -> event.getNpc().equals(npc) && event.getEditor() == editor);
    }

    @Test
    void signedNpcSkinsComeBackToTheNpc() {
        UUID npc = UUID.randomUUID();
        skins.dressable.add(npc);

        router.signed(npc, WearerKind.NPC, SIGNED);

        assertEquals(Optional.of(SIGNED), skins.current(npc));
    }

    @Test
    void ownersNeedAnNpcPermission() {
        PlayerMock owner = server.addPlayer();
        NpcInfo info = new NpcInfo(UUID.randomUUID(), 1, "Baker", Optional.of(owner.getUniqueId()));

        assertFalse(npcs.access().canOpen(owner, info));
    }

    @Test
    void anEmptiedRemovedNpcEntryIsDiscardedWhenItsMenuCloses() throws java.io.IOException {
        NpcInfo baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.empty());
        org.skysongdev.skysongwardrobe.wardrobe.Wardrobe worn = new org.skysongdev.skysongwardrobe.wardrobe.Wardrobe(baker.id());
        worn.put("coat", new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE));
        NpcArchive archive = new NpcArchive(folder.resolve("npcs/removed"), Logger.getAnonymousLogger());
        UUID entry = archive.store(baker, worn, java.time.Instant.now());
        WardrobeKinds.Kind removed = kinds.get(WardrobeKind.REMOVED_NPC).orElseThrow();
        removed.wardrobes().load(entry).put("coat", null);

        removed.changed().accept(entry, server.addPlayer());

        assertEquals(0, archive.list().size());
    }
}
