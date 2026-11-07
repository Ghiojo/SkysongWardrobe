package org.skysongdev.skysongwardrobe.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;
import org.skysongdev.skysongwardrobe.gallery.SkinId;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryMenuTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock viewer;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        viewer = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void showsEntriesAsTheirIconsWithNameAndId() {
        GalleryMenu menu = menu(List.of(entry("cape", Material.RED_BANNER), entry("hood", null)));

        ItemStack cape = menu.getInventory().getItem(0);
        assertEquals(Material.RED_BANNER, cape.getType());
        assertEquals("Name of cape", plain(cape.getItemMeta().displayName()));
        assertEquals("cape", plain(cape.getItemMeta().lore().getFirst()));
        assertEquals(GalleryMenu.DEFAULT_ICON, menu.getInventory().getItem(1).getType());
        assertNull(menu.getInventory().getItem(2));
    }

    @Test
    void bottomRowIsFillerWithArrowsOnlyWhereThereIsAPage() {
        GalleryMenu menu = menu(entries(100));

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(GalleryMenu.PREVIOUS_SLOT).getType());
        assertEquals(Material.ARROW, menu.getInventory().getItem(GalleryMenu.NEXT_SLOT).getType());

        menu.click(GalleryMenu.NEXT_SLOT);
        menu.click(GalleryMenu.NEXT_SLOT);

        assertEquals(2, menu.page());
        assertEquals(Material.ARROW, menu.getInventory().getItem(GalleryMenu.PREVIOUS_SLOT).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(GalleryMenu.NEXT_SLOT).getType());
        assertEquals("Name of e090", plain(menu.getInventory().getItem(0).getItemMeta().displayName()));
        assertNull(menu.getInventory().getItem(10));
    }

    @Test
    void clicksPastTheEndsDoNothing() {
        GalleryMenu menu = menu(entries(3));

        menu.click(GalleryMenu.PREVIOUS_SLOT);
        menu.click(GalleryMenu.NEXT_SLOT);

        assertEquals(0, menu.page());
    }

    @Test
    void titleNamesTheOwner() {
        menu(List.of()).open(viewer);

        assertSame(GalleryMenu.class, viewer.getOpenInventory().getTopInventory().getHolder(false).getClass());
        assertEquals("Alice's gallery", PlainTextComponentSerializer.plainText().serialize(viewer.getOpenInventory().title()));
    }

    @Test
    void listenerCancelsClicksAndTurnsPages() {
        GalleryMenu menu = menu(entries(50));
        menu.open(viewer);

        InventoryClickEvent onEntry = click(0);
        InventoryClickEvent onArrow = click(GalleryMenu.NEXT_SLOT);

        assertTrue(onEntry.isCancelled());
        assertTrue(onArrow.isCancelled());
        assertEquals(1, menu.page());
    }

    private InventoryClickEvent click(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(viewer.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private GalleryMenu menu(List<SkinEntry> entries) {
        return new GalleryMenu("Alice", entries, plugin.configuration().galleryMenu(), plugin.configuration().messages());
    }

    private static List<SkinEntry> entries(int count) {
        return IntStream.range(0, count).mapToObj(i -> entry("e%03d".formatted(i), null)).toList();
    }

    private static SkinEntry entry(String id, Material icon) {
        return new SkinEntry(new SkinId(id), "Name of " + id, Optional.ofNullable(icon),
                URI.create("https://example.com/" + id + ".png"), Instant.EPOCH);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
