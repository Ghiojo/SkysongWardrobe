package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

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
