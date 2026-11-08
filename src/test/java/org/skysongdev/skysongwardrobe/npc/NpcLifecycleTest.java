package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NpcLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-11-08T16:00:00Z");
    private static final SignedTexture BASE = new SignedTexture("base", "sig");

    @TempDir
    Path folder;
    private ServerMock server;
    private NpcRepository records;
    private WardrobeRepository wardrobes;
    private final RenderQueue queue = mock(RenderQueue.class);
    private NpcLifecycle lifecycle;
    private NpcInfo baker;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        MockBukkit.load(SkysongWardrobe.class);
        records = new NpcRepository(folder.resolve("npcs"), Logger.getAnonymousLogger());
        wardrobes = new WardrobeRepository(folder.resolve("npcs/wardrobes"), Logger.getAnonymousLogger());
        lifecycle = new NpcLifecycle(records, wardrobes, new NpcArchive(folder.resolve("npcs/removed"),
                Logger.getAnonymousLogger()), queue, server, Clock.fixed(NOW, ZoneOffset.UTC), Logger.getAnonymousLogger());
        baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.empty());
        records.save(new NpcRecord(baker.id(), BASE));
        wardrobes.load(baker.id()).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        wardrobes.save(wardrobes.load(baker.id()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aRemovedNpcsWardrobeIsArchivedWithWhoItWas() throws IOException {
        lifecycle.removed(baker);

        Path removed = folder.resolve("npcs/removed");
        Path infoFile;
        try (var files = Files.list(removed)) {
            infoFile = files.filter(path -> path.toString().endsWith(NpcArchive.INFO_SUFFIX)).findFirst().orElseThrow();
        }
        YamlConfiguration info = YamlConfiguration.loadConfiguration(infoFile.toFile());
        assertEquals(baker.id().toString(), info.getString("npc"));
        assertEquals("Baker", info.getString("name"));
        assertEquals(7, info.getInt("number"));
        assertEquals(NOW.toString(), info.getString("removed-at"));
        String name = infoFile.getFileName().toString();
        UUID entry = UUID.fromString(name.substring(0, name.length() - NpcArchive.INFO_SUFFIX.length()));
        assertEquals(Material.LEATHER_CHESTPLATE, new WardrobeRepository(removed, Logger.getAnonymousLogger())
                .load(entry).item("coat").orElseThrow().getType());
    }

    @Test
    void aRemovedNpcLeavesNoLiveFilesOrRenderBehind() {
        lifecycle.removed(baker);

        assertTrue(wardrobes.load(baker.id()).items().isEmpty());
        assertFalse(Files.exists(folder.resolve("npcs/wardrobes/" + baker.id() + ".yml")));
        assertEquals(Optional.empty(), records.find(baker.id()));
        verify(queue).cancel(baker.id());
    }

    @Test
    void anUndressedNpcIsNotArchived() {
        wardrobes.load(baker.id()).put("coat", null);

        lifecycle.removed(baker);

        assertFalse(Files.exists(folder.resolve("npcs/removed")));
    }

    @Test
    void removingAnNpcClosesItsOpenMenu() {
        PlayerMock staff = server.addPlayer();
        staff.openInventory(new WardrobeMenu(WardrobeKind.NPC, wardrobes.load(baker.id()), new WardrobeLayout("Wardrobe",
                Material.GRAY_STAINED_GLASS_PANE, " ", List.of(new SlotDefinition("coat", 10, Set.of("coat"), 30))),
                new FakeItemTags()).getInventory());

        lifecycle.removed(baker);

        assertTrue(WardrobeMenu.of(staff.getOpenInventory()).isEmpty());
    }

    @Test
    void aCopyWearsTheSameOutfitOnTheSameBase() {
        UUID copy = UUID.randomUUID();

        lifecycle.copied(baker.id(), copy);

        assertEquals(Optional.of(new NpcRecord(copy, BASE)), records.find(copy));
        assertEquals(Material.LEATHER_CHESTPLATE, new WardrobeRepository(folder.resolve("npcs/wardrobes"),
                Logger.getAnonymousLogger()).load(copy).item("coat").orElseThrow().getType());
        assertTrue(wardrobes.load(baker.id()).item("coat").isPresent());
    }
}
