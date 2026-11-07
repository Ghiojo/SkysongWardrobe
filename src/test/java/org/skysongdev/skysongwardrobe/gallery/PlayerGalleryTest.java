package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerGalleryTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final byte[] PNG = {1, 2, 3};
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    @TempDir
    Path root;
    private Path folder;

    @BeforeEach
    void setUp() {
        // Material.matchMaterial needs a registry, which MockBukkit provides.
        MockBukkit.mock();
        folder = root.resolve("Steve");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void addWritesTheImageAndSurvivesAReload() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        SkinEntry cape = entry("cape").withIcon(Material.RED_BANNER);

        assertTrue(gallery.add(cape, PNG));

        assertArrayEquals(PNG, Files.readAllBytes(folder.resolve("cape.png")));
        PlayerGallery reloaded = PlayerGallery.load(folder, OWNER, LOGGER);
        assertEquals(Optional.of(cape), reloaded.entry(new SkinId("cape")));
        assertEquals(OWNER.toString(),
                YamlConfiguration.loadConfiguration(folder.resolve("gallery.yml").toFile()).getString("uuid"));
    }

    @Test
    void addRefusesATakenId() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        gallery.add(entry("cape"), PNG);

        assertFalse(gallery.add(entry("cape").withName("Other"), new byte[] {9}));
        assertArrayEquals(PNG, Files.readAllBytes(gallery.imageFile(new SkinId("cape"))));
        assertEquals("cape", gallery.entry(new SkinId("cape")).orElseThrow().name());
    }

    @Test
    void entriesAreOrderedById() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        gallery.add(entry("zed"), PNG);
        gallery.add(entry("alpha"), PNG);

        assertEquals(List.of("alpha", "zed"), gallery.entries().stream().map(e -> e.id().value()).toList());
    }

    @Test
    void updateChangesOnlyExistingEntries() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        gallery.add(entry("cape"), PNG);

        assertTrue(gallery.update(entry("cape").withName("Red cape")));
        assertFalse(gallery.update(entry("hood")));

        assertEquals("Red cape", PlayerGallery.load(folder, OWNER, LOGGER)
                .entry(new SkinId("cape")).orElseThrow().name());
    }

    @Test
    void replaceSwapsTheImageOfAnExistingEntry() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        gallery.add(entry("cape"), PNG);

        assertTrue(gallery.replace(entry("cape").withName("New"), new byte[] {9}));
        assertFalse(gallery.replace(entry("hood"), new byte[] {9}));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(gallery.imageFile(new SkinId("cape"))));
        assertFalse(Files.exists(gallery.imageFile(new SkinId("hood"))));
    }

    @Test
    void removeKeepsTheImageSoWornItemsStillRender() throws IOException {
        PlayerGallery gallery = PlayerGallery.load(folder, OWNER, LOGGER);
        gallery.add(entry("cape"), PNG);

        assertTrue(gallery.remove(new SkinId("cape")));
        assertFalse(gallery.remove(new SkinId("cape")));

        assertTrue(PlayerGallery.load(folder, OWNER, LOGGER).entries().isEmpty());
        assertTrue(Files.isRegularFile(gallery.imageFile(new SkinId("cape"))));
    }

    @Test
    void skipsBrokenEntriesWhenReading() throws IOException {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("gallery.yml"), """
                uuid: %s
                entries:
                  good: {name: Good, link: "https://example.com/good.png"}
                  bad-id: {name: Bad, link: "https://example.com/bad.png"}
                  nolink: {name: No link}
                """.formatted(OWNER));

        assertEquals(List.of("good"), PlayerGallery.load(folder, OWNER, LOGGER)
                .entries().stream().map(e -> e.id().value()).toList());
    }

    @Test
    void missingFolderIsAnEmptyGallery() {
        assertTrue(PlayerGallery.load(folder, OWNER, LOGGER).entries().isEmpty());
        assertFalse(Files.exists(folder));
    }

    private static SkinEntry entry(String id) {
        return new SkinEntry(new SkinId(id), id, Optional.empty(), URI.create("https://example.com/" + id + ".png"), Instant.parse("2026-11-06T12:00:00Z"));
    }
}
