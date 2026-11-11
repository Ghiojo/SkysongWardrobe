package org.skysongdev.skysongwardrobe.wardrobe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeOpenerTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private PlayerMock alice;
    private PlayerMock bob;
    private WardrobeOpener opener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer();
        bob = server.addPlayer();
        WardrobeKinds kinds = new WardrobeKinds()
                .register(WardrobeKind.PLAYER, repository("players"), (owner, editor) -> { })
                .register(WardrobeKind.NPC, repository("npcs"), (npc, editor) -> { });
        opener = new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private WardrobeRepository repository(String name) {
        return new WardrobeRepository(folder.resolve(name), Logger.getAnonymousLogger());
    }

    @Test
    void opensTheWearersWardrobeOfThatKind() {
        UUID npc = UUID.randomUUID();

        assertEquals(OpenOutcome.OPENED, opener.open(alice, WardrobeKind.NPC, npc, OpenSource.COMMAND));

        assertTrue(WardrobeMenu.of(alice.getOpenInventory()).filter(menu -> menu.shows(WardrobeKind.NPC, npc)).isPresent());
    }

    @Test
    void anExclusiveWearerOpensForOnePlayerAtATime() {
        UUID npc = UUID.randomUUID();
        opener.open(alice, WardrobeKind.NPC, npc, OpenSource.COMMAND);

        assertEquals(OpenOutcome.IN_USE, opener.open(bob, WardrobeKind.NPC, npc, OpenSource.COMMAND));
        assertFalse(WardrobeMenu.of(bob.getOpenInventory()).isPresent());
    }

    @Test
    void aPlayersWardrobeMayBeOpenTwice() {
        opener.open(alice, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.COMMAND);

        assertEquals(OpenOutcome.OPENED, opener.open(bob, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.COMMAND));
    }

    @Test
    void aKindNothingStoresIsAMistake() {
        assertThrows(IllegalStateException.class,
                () -> opener.open(alice, WardrobeKind.MANNEQUIN, UUID.randomUUID(), OpenSource.COMMAND));
    }

    @Test
    void eachRefusalHasItsWearersMessage() {
        assertEquals(Optional.empty(), WardrobeOpener.messageKey(WardrobeKind.NPC, OpenOutcome.OPENED));
        assertEquals(Optional.of("wardrobe.closed"), WardrobeOpener.messageKey(WardrobeKind.NPC, OpenOutcome.WARDROBE_CLOSED));
        assertEquals(Optional.of("npc.in-use"), WardrobeOpener.messageKey(WardrobeKind.NPC, OpenOutcome.IN_USE));
        assertEquals(Optional.of("mannequin.no-access"),
                WardrobeOpener.messageKey(WardrobeKind.MANNEQUIN, OpenOutcome.NO_ACCESS));
        assertEquals(Optional.of("npc.not-a-player"),
                WardrobeOpener.messageKey(WardrobeKind.NPC, OpenOutcome.NOT_A_PLAYER_NPC));
    }

    @Test
    void openingTellsNothing() {
        opener.tell(alice, WardrobeKind.NPC, OpenOutcome.OPENED);

        assertEquals(null, alice.nextActionBar());
        assertEquals(null, alice.nextComponentMessage());
    }
}
