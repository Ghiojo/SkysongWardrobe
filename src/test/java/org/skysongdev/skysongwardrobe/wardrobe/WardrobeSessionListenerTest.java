package org.skysongdev.skysongwardrobe.wardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeSessionListenerTest {
    private ServerMock server;
    private SkysongWardrobe plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theWardrobeIsReadWhileThePlayerLogsIn() throws Exception {
        UUID id = UUID.randomUUID();
        AsyncPlayerPreLoginEvent login = new AsyncPlayerPreLoginEvent("Alex", InetAddress.getLoopbackAddress(), id);

        CompletableFuture.runAsync(() -> server.getPluginManager().callEvent(login)).join();

        assertTrue(plugin.wardrobes().loaded(id).isPresent());
    }

    @Test
    void aRefusedLoginReadsNothing() throws Exception {
        UUID id = UUID.randomUUID();
        AsyncPlayerPreLoginEvent login = new AsyncPlayerPreLoginEvent("Alex", InetAddress.getLoopbackAddress(), id);
        login.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text("banned"));

        CompletableFuture.runAsync(() -> server.getPluginManager().callEvent(login)).join();

        assertTrue(plugin.wardrobes().loaded(id).isEmpty());
    }

    @Test
    void joinFiresTheUpdateEvent() {
        PlayerMock player = server.addPlayer();

        server.getPluginManager().assertEventFired(WardrobeUpdateEvent.class, event -> event.getPlayer() == player);
    }

    @Test
    void quitDropsTheCachedWardrobe() {
        PlayerMock player = server.addPlayer();
        Wardrobe loaded = plugin.wardrobes().load(player.getUniqueId());

        player.disconnect();

        assertNotSame(loaded, plugin.wardrobes().load(player.getUniqueId()));
    }

    @Test
    void quitKeepsTheWardrobeWhileStaffHaveItOpen() {
        PlayerMock owner = server.addPlayer();
        PlayerMock staff = server.addPlayer();
        Wardrobe loaded = plugin.wardrobes().load(owner.getUniqueId());
        loaded.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        staff.openInventory(new WardrobeMenu(loaded, plugin.configuration().layout().orElseThrow(), new FakeItemTags())
                .getInventory());

        owner.disconnect();

        assertSame(loaded, plugin.wardrobes().load(owner.getUniqueId()));
    }
}
