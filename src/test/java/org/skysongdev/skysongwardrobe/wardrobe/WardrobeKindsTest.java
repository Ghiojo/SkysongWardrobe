package org.skysongdev.skysongwardrobe.wardrobe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeKindsTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private WardrobeRepository players;
    private WardrobeRepository mannequins;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        players = new WardrobeRepository(folder.resolve("players"), Logger.getAnonymousLogger());
        mannequins = new WardrobeRepository(folder.resolve("mannequins"), Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playersAndMannequinsUseTheirOwnStorage() {
        WardrobeKinds kinds = WardrobeKinds.of(players, mannequins, server);

        assertSame(players, kinds.get(WardrobeKind.PLAYER).orElseThrow().wardrobes());
        assertSame(mannequins, kinds.get(WardrobeKind.MANNEQUIN).orElseThrow().wardrobes());
        assertFalse(kinds.get(WardrobeKind.NPC).isPresent());
    }

    @Test
    void aChangedPlayerWardrobeAsksForTheOnlineOwnersRender() {
        PlayerMock owner = server.addPlayer();

        WardrobeKinds.of(players, mannequins, server).get(WardrobeKind.PLAYER).orElseThrow()
                .changed().accept(owner.getUniqueId(), owner);

        server.getPluginManager().assertEventFired(WardrobeUpdateEvent.class, event -> event.getPlayer() == owner);
    }

    @Test
    void anOfflineOwnerGetsNoEvent() {
        WardrobeKinds.of(players, mannequins, server).get(WardrobeKind.PLAYER).orElseThrow()
                .changed().accept(UUID.randomUUID(), server.addPlayer());

        server.getPluginManager().assertEventNotFired(WardrobeUpdateEvent.class);
    }

    @Test
    void aChangedMannequinWardrobeAsksForTheMannequinsRender() {
        UUID mannequin = UUID.randomUUID();

        WardrobeKinds.of(players, mannequins, server).get(WardrobeKind.MANNEQUIN).orElseThrow()
                .changed().accept(mannequin, server.addPlayer());

        server.getPluginManager().assertEventFired(MannequinUpdateEvent.class,
                event -> event.getMannequin().equals(mannequin));
    }

    @Test
    void aNewKindRegistersItsOwnStorageAndHandler() {
        WardrobeRepository npcs = new WardrobeRepository(folder.resolve("npcs"), Logger.getAnonymousLogger());
        List<String> changed = new ArrayList<>();
        UUID npc = UUID.randomUUID();
        PlayerMock editor = server.addPlayer();

        WardrobeKinds kinds = WardrobeKinds.of(players, mannequins, server)
                .register(WardrobeKind.NPC, npcs, (wearer, by) -> changed.add(wearer + " by " + by.getName()));
        kinds.get(WardrobeKind.NPC).orElseThrow().changed().accept(npc, editor);

        assertSame(npcs, kinds.get(WardrobeKind.NPC).orElseThrow().wardrobes());
        assertEquals(List.of(npc + " by " + editor.getName()), changed);
    }

    @Test
    void onlyMannequinMenusHaveEquipment() {
        assertTrue(WardrobeKind.MANNEQUIN.hasEquipment());
        assertFalse(WardrobeKind.PLAYER.hasEquipment());
        assertFalse(WardrobeKind.NPC.hasEquipment());
    }
}
