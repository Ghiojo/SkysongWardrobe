package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeletedSkinArchiveTest {
    private static final SkinId CAPE = new SkinId("cape");
    private static final byte[] PNG = {1, 2, 3};

    @TempDir
    Path root;
    private DeletedSkinArchive archive;
    private PlayerGallery gallery;

    @BeforeEach
    void setUp() throws IOException {
        archive = new DeletedSkinArchive(root.resolve("deleted"));
        gallery = PlayerGallery.load(root.resolve("skins").resolve("Steve"), UUID.randomUUID(), Logger.getAnonymousLogger());
        gallery.add(entry(), PNG);
    }

    @Test
    void copiesTheImageAndRemovesTheEntry() throws IOException {
        Optional<Path> copy = archive.delete(gallery, CAPE);

        assertEquals(Optional.of(root.resolve("deleted").resolve("Steve-cape.png")), copy);
        assertArrayEquals(PNG, Files.readAllBytes(copy.orElseThrow()));
        assertTrue(gallery.entry(CAPE).isEmpty());
        assertTrue(Files.isRegularFile(gallery.imageFile(CAPE)), "the original stays for worn items");
    }

    @Test
    void numbersLaterCopiesOfTheSameId() throws IOException {
        archive.delete(gallery, CAPE);
        gallery.add(entry(), PNG);
        archive.delete(gallery, CAPE);
        gallery.add(entry(), PNG);

        assertEquals(Optional.of(root.resolve("deleted").resolve("Steve-cape_3.png")), archive.delete(gallery, CAPE));
        assertTrue(Files.isRegularFile(root.resolve("deleted").resolve("Steve-cape_2.png")));
    }

    @Test
    void archiveOnlyCopies() throws IOException {
        assertEquals(Optional.of(root.resolve("deleted").resolve("Steve-cape.png")), archive.archive(gallery, CAPE));
        assertTrue(gallery.entry(CAPE).isPresent());
    }

    @Test
    void unknownIdChangesNothing() throws IOException {
        assertEquals(Optional.empty(), archive.delete(gallery, new SkinId("hood")));
        assertTrue(Files.notExists(root.resolve("deleted")));
    }

    @Test
    void entryWhoseImageIsMissingIsStillRemoved() throws IOException {
        Files.delete(gallery.imageFile(CAPE));

        assertEquals(Optional.empty(), archive.delete(gallery, CAPE));
        assertTrue(gallery.entry(CAPE).isEmpty());
    }

    private static SkinEntry entry() {
        return new SkinEntry(CAPE, "Cape", Optional.empty(), URI.create("https://example.com/cape.png"), Instant.parse("2026-11-06T12:00:00Z"));
    }
}
