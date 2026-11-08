package org.skysongdev.skysongwardrobe.npc;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemovedNpcsMenuTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private NpcArchive archive;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        archive = new NpcArchive(folder, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private UUID remove(String name, int number, Instant when) throws IOException {
        NpcInfo npc = new NpcInfo(UUID.randomUUID(), number, name, Optional.empty());
        Wardrobe wardrobe = new Wardrobe(npc.id());
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        return archive.store(npc, wardrobe, when);
    }

    private RemovedNpcsMenu menu() {
        return new RemovedNpcsMenu(archive, plugin.configuration().galleryMenu(), plugin.configuration().layout().orElseThrow(),
                new FakeItemTags(), plugin.configuration().messages());
    }

    @Test
    void showsEachRemovedNpcWithItsNumberAndDate() throws IOException {
        remove("Baker", 7, Instant.parse("2026-11-08T16:30:00Z"));

        ItemStack icon = menu().getInventory().getItem(0);

        assertEquals("Baker #7", PlainTextComponentSerializer.plainText().serialize(icon.getItemMeta().displayName()));
        assertEquals("Removed 2026-11-08 16:30 UTC",
                PlainTextComponentSerializer.plainText().serialize(icon.getItemMeta().lore().getFirst()));
    }

    @Test
    void clickingAnEntryOpensItsWardrobeTakeOutOnly() throws IOException {
        UUID entry = remove("Baker", 7, Instant.now());
        PlayerMock staff = server.addPlayer();

        menu().click(0, staff);

        WardrobeMenu opened = WardrobeMenu.of(staff.getOpenInventory()).orElseThrow();
        assertTrue(opened.shows(WardrobeKind.REMOVED_NPC, entry));
    }

    @Test
    void pagesHold45Entries() throws IOException {
        for (int number = 0; number < 46; number++) {
            remove("Npc" + number, number, Instant.ofEpochSecond(1_000_000 + number));
        }
        RemovedNpcsMenu menu = menu();
        PlayerMock staff = server.addPlayer();

        menu.click(RemovedNpcsMenu.NEXT_SLOT, staff);

        assertEquals("Npc0 #0", PlainTextComponentSerializer.plainText()
                .serialize(menu.getInventory().getItem(0).getItemMeta().displayName()));
        assertNull(menu.getInventory().getItem(1));
    }
}
