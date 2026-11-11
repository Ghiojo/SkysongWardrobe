package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.NpcView;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcsImplTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock owner;
    private PlayerMock clerk;
    private NpcInfo baker;
    private NpcsImpl npcs;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        owner = server.addPlayer();
        clerk = server.addPlayer();
        owner.addAttachment(plugin, "skysongwardrobe.npc", true);
        baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.of(owner.getUniqueId()));
        WardrobeKinds kinds = new WardrobeKinds().register(WardrobeKind.NPC,
                new WardrobeRepository(folder, Logger.getAnonymousLogger()), (npc, editor) -> { });
        NpcHooks hooks = NpcFixture.hooks(new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server),
                () -> plugin.configuration().settings().npcPermissions(), baker);
        npcs = new NpcsImpl(new MainThread(() -> true), () -> Optional.of(hooks));
        server.getPluginManager().registerEvents(npcs, plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void findsNpcsByNumberAndId() {
        NpcView view = new NpcView(baker.id(), 7, "Baker", Optional.of(owner.getUniqueId()));

        assertTrue(npcs.available());
        assertEquals(Optional.of(view), npcs.find(7));
        assertEquals(Optional.of(view), npcs.find(baker.id()));
        assertEquals(Optional.empty(), npcs.find(8));
    }

    @Test
    void theOwnerMayOpenAndOthersMayNot() {
        assertTrue(npcs.canOpenWardrobe(owner, 7));
        assertFalse(npcs.canOpenWardrobe(clerk, baker.id()));
        assertFalse(npcs.canOpenWardrobe(owner, 8));
    }

    @Test
    void aRegisteredRuleLetsMorePlayersIn() {
        Plugin shops = MockBukkit.createMockPlugin("Shops");
        npcs.registerAccessRule(shops, (viewer, npc) -> viewer.equals(clerk) && npc.name().equals("Baker"));

        assertTrue(npcs.canOpenWardrobe(clerk, 7));
    }

    @Test
    void rulesLeaveWithTheirPlugin() {
        Plugin shops = MockBukkit.createMockPlugin("Shops");
        npcs.registerAccessRule(shops, (viewer, npc) -> true);

        server.getPluginManager().disablePlugin(shops);

        assertFalse(npcs.canOpenWardrobe(clerk, 7));
    }

    @Test
    void unregisteringRemovesOnlyThatPluginsRules() {
        Plugin shops = MockBukkit.createMockPlugin("Shops");
        Plugin guilds = MockBukkit.createMockPlugin("Guilds");
        npcs.registerAccessRule(shops, (viewer, npc) -> true);
        npcs.registerAccessRule(guilds, (viewer, npc) -> viewer.equals(clerk));

        npcs.unregisterAccessRules(shops);

        assertTrue(npcs.canOpenWardrobe(clerk, 7));
        assertFalse(npcs.canOpenWardrobe(server.addPlayer(), 7));
    }

    @Test
    void withoutCitizensNothingIsFoundOrOpens() {
        assertFalse(SkysongWardrobeApi.get().npcs().available());
        assertEquals(Optional.empty(), SkysongWardrobeApi.get().npcs().find(7));
        assertFalse(SkysongWardrobeApi.get().npcs().canOpenWardrobe(owner, 7));
    }
}
