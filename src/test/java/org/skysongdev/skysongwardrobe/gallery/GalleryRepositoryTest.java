package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryRepositoryTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @TempDir
    Path root;
    private GalleryFolders folders;
    private GalleryRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        folders = new GalleryFolders(root, player -> Optional.empty(), Logger.getAnonymousLogger());
        folders.scan();
        repository = new GalleryRepository(folders, Logger.getAnonymousLogger());
    }

    @Test
    void sameOwnerGetsTheSameGallery() throws IOException {
        PlayerGallery first = repository.claim(ALICE, "Alice");

        assertSame(first, repository.claim(ALICE, "Alice"));
        assertSame(first, repository.existing(ALICE).orElseThrow());
    }

    @Test
    void ownerWithoutAFolderHasNoExistingGallery() {
        assertTrue(repository.existing(BOB).isEmpty());
    }

    @Test
    void movedFolderIsReloadedFromItsNewPlace() throws IOException {
        PlayerGallery alice = repository.claim(ALICE, "Steve");
        alice.add(new SkinEntry(new SkinId("cape"), "Cape", Optional.empty(), URI.create("https://e.com/c.png"), Instant.parse("2026-11-06T12:00:00Z")), new byte[] {1});

        repository.claim(BOB, "Steve");

        PlayerGallery moved = repository.existing(ALICE).orElseThrow();
        assertEquals(root.resolve(ALICE.toString()), moved.folder());
        assertTrue(moved.entry(new SkinId("cape")).isPresent());
    }
}
