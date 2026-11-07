package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenuTest.BOOTS;
import static org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenuTest.COAT;

class WardrobeMenuListenerTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private PlayerMock player;
    private WardrobeRepository wardrobes;
    private WardrobeMenu menu;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer();
        wardrobes = new WardrobeRepository(folder, Logger.getAnonymousLogger());
        server.getPluginManager().registerEvents(
                new WardrobeMenuListener(wardrobes, plugin.configuration(), server, Logger.getAnonymousLogger()), plugin);
        menu = new WardrobeMenu(wardrobes.load(player.getUniqueId()), WardrobeMenuTest.layout(), WardrobeMenuTest.tags());
        player.openInventory(menu.getInventory());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cursorItemGoesInOneAtATimeAndSaves() {
        view().setCursor(new ItemStack(Material.LEATHER_CHESTPLATE, 2));

        assertTrue(click(COAT, ClickType.LEFT, InventoryAction.PLACE_ALL).isCancelled());

        assertEquals(1, view().getCursor().getAmount());
        assertEquals(Material.LEATHER_CHESTPLATE, menu.getInventory().getItem(COAT).getType());
        assertTrue(Files.isRegularFile(folder.resolve(player.getUniqueId() + ".yml")));
    }

    @Test
    void refusedItemStaysOnTheCursorWithAMessage() {
        view().setCursor(new ItemStack(Material.DIAMOND));

        click(COAT, ClickType.LEFT, InventoryAction.PLACE_ALL);

        assertEquals(Material.DIAMOND, view().getCursor().getType());
        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void clickingAStoredItemReturnsItToTheInventory() {
        menu.placeInto(BOOTS, new ItemStack(Material.LEATHER_BOOTS));

        click(BOOTS, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertTrue(player.getInventory().contains(Material.LEATHER_BOOTS));
        assertNull(menu.getInventory().getItem(BOOTS));
    }

    @Test
    void shiftClickFromTheInventoryUsesTheSlotRules() {
        player.getInventory().setItem(0, new ItemStack(Material.LEATHER_BOOTS, 2));
        InventoryClickEvent event = new InventoryClickEvent(view(), InventoryType.SlotType.QUICKBAR, rawSlotOfHotbar(0),
                ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(1, player.getInventory().getItem(0).getAmount());
        assertEquals(Material.LEATHER_BOOTS, menu.getInventory().getItem(BOOTS).getType());
    }

    @Test
    void fillerAndNumberKeysAndDragsAreRefused() {
        assertTrue(click(0, ClickType.LEFT, InventoryAction.PICKUP_ALL).isCancelled());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(0).getType());

        player.getInventory().setItem(0, new ItemStack(Material.LEATHER_CHESTPLATE));
        assertTrue(click(COAT, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP).isCancelled());
        assertNull(menu.getInventory().getItem(COAT));

        InventoryDragEvent drag = new InventoryDragEvent(view(), null, new ItemStack(Material.LEATHER_CHESTPLATE),
                false, Map.of(COAT, new ItemStack(Material.LEATHER_CHESTPLATE)));
        server.getPluginManager().callEvent(drag);
        assertEquals(Event.Result.DENY, drag.getResult());
    }

    @Test
    void closingAChangedMenuFiresTheUpdateEventForTheOwner() {
        player.closeInventory();
        server.getPluginManager().assertEventNotFired(WardrobeUpdateEvent.class);

        player.openInventory(menu.getInventory());
        menu.placeInto(COAT, new ItemStack(Material.LEATHER_CHESTPLATE));
        player.closeInventory();

        server.getPluginManager().assertEventFired(WardrobeUpdateEvent.class, event -> event.getPlayer() == player);
    }

    private InventoryClickEvent click(int slot, ClickType type, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(view(), InventoryType.SlotType.CONTAINER, slot, type, action,
                type == ClickType.NUMBER_KEY ? 0 : -1);
        server.getPluginManager().callEvent(event);
        return event;
    }

    /** Raw slots run through the menu, then the 27 storage slots, then the hotbar. */
    private int rawSlotOfHotbar(int hotbar) {
        return view().getTopInventory().getSize() + 27 + hotbar;
    }

    private InventoryView view() {
        return player.getOpenInventory();
    }
}
