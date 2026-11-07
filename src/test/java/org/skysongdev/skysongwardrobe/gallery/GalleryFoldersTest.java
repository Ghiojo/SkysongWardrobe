package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryFoldersTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @TempDir
    Path root;
    private final Map<UUID, String> mojang = new HashMap<>();
    private final NameLookup lookup = player -> Optional.ofNullable(mojang.get(player));

    @Test
    void scanFindsFoldersByTheUuidInside() throws IOException {
        galleryFolder("Steve", ALICE);
        Files.createDirectories(root.resolve("NoIndex"));

        GalleryFolders folders = scanned();

        assertEquals(Optional.of(root.resolve("Steve")), folders.existing(ALICE));
        assertEquals(Optional.empty(), folders.existing(BOB));
    }

    @Test
    void newPlayerGetsAFolderNamedAfterThemWithoutCreatingIt() throws IOException {
        GalleryFolders folders = scanned();

        assertEquals(root.resolve("Alice"), folders.claim(ALICE, "Alice"));
        assertFalse(Files.exists(root.resolve("Alice")));
        assertEquals(Optional.of(root.resolve("Alice")), folders.existing(ALICE));
    }

    @Test
    void ownerWithANewNameGetsTheirFolderRenamed() throws IOException {
        galleryFolder("OldAlice", ALICE);
        GalleryFolders folders = scanned();

        Path folder = folders.claim(ALICE, "Alice");

        assertEquals(root.resolve("Alice"), folder);
        assertTrue(Files.isRegularFile(folder.resolve("gallery.yml")));
        assertFalse(Files.exists(root.resolve("OldAlice")));
    }

    @Test
    void takenNameMovesTheOldOwnerToTheirCurrentName() throws IOException {
        galleryFolder("Steve", ALICE);
        mojang.put(ALICE, "Alice");
        GalleryFolders folders = scanned();

        folders.claim(BOB, "Steve");

        assertEquals(Optional.of(root.resolve("Alice")), folders.existing(ALICE));
        assertTrue(Files.isRegularFile(root.resolve("Alice").resolve("gallery.yml")));
        assertEquals(Optional.of(root.resolve("Steve")), folders.existing(BOB));
    }

    @Test
    void takenNameMovesTheOldOwnerToTheirUuidWhenLookupFails() throws IOException {
        galleryFolder("Steve", ALICE);
        GalleryFolders folders = scanned();

        folders.claim(BOB, "Steve");

        assertEquals(Optional.of(root.resolve(ALICE.toString())), folders.existing(ALICE));
        assertTrue(Files.isDirectory(root.resolve(ALICE.toString())));
    }

    @Test
    void staleLookupStillMovesTheOldOwnerOutOfTheWay() throws IOException {
        galleryFolder("Steve", ALICE);
        mojang.put(ALICE, "steve");
        GalleryFolders folders = scanned();

        folders.claim(BOB, "Steve");

        assertEquals(Optional.of(root.resolve(ALICE.toString())), folders.existing(ALICE));
    }

    @Test
    void uuidFolderIsRenamedWhenItsOwnerJoins() throws IOException {
        galleryFolder(ALICE.toString(), ALICE);
        GalleryFolders folders = scanned();

        assertEquals(root.resolve("Alice"), folders.claim(ALICE, "Alice"));
        assertTrue(Files.isDirectory(root.resolve("Alice")));
    }

    @Test
    void changeOfCaseRenamesTheFolder() throws IOException {
        galleryFolder("alice", ALICE);
        GalleryFolders folders = scanned();

        folders.claim(ALICE, "Alice");

        assertEquals(Optional.of(root.resolve("Alice")), folders.existing(ALICE));
        assertTrue(Files.isRegularFile(root.resolve("Alice").resolve("gallery.yml")));
    }

    private GalleryFolders scanned() throws IOException {
        GalleryFolders folders = new GalleryFolders(root, lookup, Logger.getAnonymousLogger());
        folders.scan();
        return folders;
    }

    private void galleryFolder(String name, UUID owner) throws IOException {
        Path folder = Files.createDirectories(root.resolve(name));
        Files.writeString(folder.resolve("gallery.yml"), "uuid: " + owner + "\n");
    }
}
