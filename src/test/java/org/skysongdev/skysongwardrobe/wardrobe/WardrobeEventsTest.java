package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.Wearer;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.api.event.WardrobeCloseEvent;
import org.skysongdev.skysongwardrobe.api.event.WardrobeOpenEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeEventsTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private PlayerMock alice;
    private WardrobeOpener opener;
    private final List<WardrobeOpenEvent> opened = new ArrayList<>();
    private final List<WardrobeCloseEvent> closed = new ArrayList<>();
    private boolean cancelOpens;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer();
        WardrobeKinds kinds = new WardrobeKinds()
                .register(WardrobeKind.PLAYER, new WardrobeRepository(folder, Logger.getAnonymousLogger()),
                        (owner, editor) -> { });
        opener = new WardrobeOpener(kinds, WardrobeMenuTest::tags, plugin.configuration(), server);
        // The plugin's own WardrobeMenuListener fires the close events.
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onOpen(WardrobeOpenEvent event) {
                opened.add(event);
                event.setCancelled(cancelOpens);
            }

            @EventHandler
            public void onClose(WardrobeCloseEvent event) {
                closed.add(event);
            }
        }, plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void openingTellsOtherPluginsWhoWhoseAndWhy() {
        opener.open(alice, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.API);

        assertEquals(1, opened.size());
        assertSame(alice, opened.get(0).getViewer());
        assertEquals(Wearer.player(alice.getUniqueId()), opened.get(0).getWearer());
        assertEquals(OpenSource.API, opened.get(0).getSource());
    }

    @Test
    void aCancelledOpenShowsNothingAndSaysNothing() {
        cancelOpens = true;

        OpenOutcome outcome = opener.open(alice, WardrobeKind.PLAYER, UUID.randomUUID(), OpenSource.COMMAND);
        opener.tell(alice, WardrobeKind.PLAYER, outcome);

        assertEquals(OpenOutcome.CANCELLED, outcome);
        assertFalse(WardrobeMenu.of(alice.getOpenInventory()).isPresent());
        assertNull(alice.nextActionBar());
    }

    @Test
    void closingUnchangedSaysSo() {
        opener.open(alice, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.COMMAND);

        alice.closeInventory();

        assertEquals(1, closed.size());
        assertEquals(Wearer.player(alice.getUniqueId()), closed.get(0).getWearer());
        assertFalse(closed.get(0).hasChanged());
    }

    @Test
    void closingAfterAChangeSaysSo() {
        opener.open(alice, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.COMMAND);
        WardrobeMenu.of(alice.getOpenInventory()).orElseThrow()
                .placeInto(WardrobeMenuTest.COAT, new ItemStack(Material.LEATHER_CHESTPLATE));

        alice.closeInventory();

        assertTrue(closed.get(0).hasChanged());
        assertSame(alice, closed.get(0).getViewer());
    }
}
