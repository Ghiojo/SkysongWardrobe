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
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcOpenerTest {
    @TempDir
    Path folder;
    private PlayerMock stranger;
    private final FakeNpcSkins skins = new FakeNpcSkins();
    private NpcInfo baker;
    private NpcOpener opener;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        stranger = server.addPlayer();
        stranger.addAttachment(plugin, "skysongwardrobe.npc", true);
        baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.of(UUID.randomUUID()));
        skins.dressable.add(baker.id());
        WardrobeKinds kinds = new WardrobeKinds().register(WardrobeKind.NPC,
                new WardrobeRepository(folder, Logger.getAnonymousLogger()), (npc, editor) -> { });
        opener = new NpcOpener(skins, NpcAccess.standard(() -> plugin.configuration().settings().npcPermissions()),
                new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theAccessRulesDecideByDefault() {
        assertEquals(OpenOutcome.NO_ACCESS, opener.open(stranger, Optional.of(baker), true));
        assertFalse(WardrobeMenu.of(stranger.getOpenInventory()).isPresent());
    }

    @Test
    void aCallerThatAlreadyDecidedSkipsTheAccessRules() {
        assertEquals(OpenOutcome.OPENED, opener.open(stranger, Optional.of(baker), false));
        assertTrue(WardrobeMenu.of(stranger.getOpenInventory()).filter(menu -> menu.shows(WardrobeKind.NPC, baker.id()))
                .isPresent());
    }

    @Test
    void skippingAccessStillNeedsAPlayerNpc() {
        skins.dressable.clear();

        assertEquals(OpenOutcome.NOT_A_PLAYER_NPC, opener.open(stranger, Optional.of(baker), false));
    }

    @Test
    void noNpcIsNotFound() {
        assertEquals(OpenOutcome.NOT_FOUND, opener.open(stranger, Optional.empty(), false));
    }
}
