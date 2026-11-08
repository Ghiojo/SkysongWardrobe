package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcArchiveTest {
    private static final Instant MONDAY = Instant.parse("2026-11-02T10:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-11-03T10:00:00Z");

    @TempDir
    Path folder;
    private NpcArchive archive;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        archive = new NpcArchive(folder, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static Wardrobe wearing(UUID npc) {
        Wardrobe wardrobe = new Wardrobe(npc);
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        return wardrobe;
    }

    @Test
    void listsEntriesNewestFirstWithWhoTheyWere() throws IOException {
        NpcInfo baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.empty());
        NpcInfo guard = new NpcInfo(UUID.randomUUID(), 8, "Guard", Optional.empty());
        UUID older = archive.store(baker, wearing(baker.id()), MONDAY);
        UUID newer = archive.store(guard, wearing(guard.id()), TUESDAY);

        assertEquals(List.of(new RemovedNpc(newer, guard.id(), 8, "Guard", TUESDAY),
                new RemovedNpc(older, baker.id(), 7, "Baker", MONDAY)), new NpcArchive(folder, Logger.getAnonymousLogger()).list());
    }

    @Test
    void anEntryIsDiscardedOnlyOnceEmpty() throws IOException {
        NpcInfo baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.empty());
        UUID entry = archive.store(baker, wearing(baker.id()), MONDAY);

        archive.discardIfEmpty(entry);
        assertEquals(1, archive.list().size());

        archive.wardrobes().load(entry).put("coat", null);
        archive.discardIfEmpty(entry);

        assertTrue(archive.list().isEmpty());
        assertFalse(Files.exists(folder.resolve(entry + ".yml")));
    }

    @Test
    void anUnreadableEntryIsSkippedAndKept() throws IOException {
        Path broken = folder.resolve(UUID.randomUUID() + YamlRemovedNpcStore.INFO_SUFFIX);
        Files.createDirectories(folder);
        Files.writeString(broken, "name: Ghost\n");

        assertTrue(archive.list().isEmpty());
        assertTrue(Files.exists(broken));
    }

    @Test
    void noFolderMeansNoEntries() {
        assertTrue(new NpcArchive(folder.resolve("missing"), Logger.getAnonymousLogger()).list().isEmpty());
    }

    @Test
    void aStoreThatCantBeReadListsNothing() {
        RemovedNpcStore failing = new RemovedNpcStore() {
            @Override
            public List<RemovedNpc> readAll() throws IOException {
                throw new IOException("database down");
            }

            @Override
            public void write(RemovedNpc entry) {
            }

            @Override
            public void delete(UUID entry) {
            }
        };

        assertTrue(new NpcArchive(failing, new WardrobeRepository(folder, Logger.getAnonymousLogger()),
                Logger.getAnonymousLogger()).list().isEmpty());
    }
}
