package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlRemovedNpcStoreTest extends RemovedNpcStoreContract {
    @TempDir
    Path folder;

    @Override
    protected RemovedNpcStore emptyStore() {
        return new YamlRemovedNpcStore(folder, Logger.getAnonymousLogger());
    }

    @Test
    void wardrobeFilesBesideTheEntriesAreNotEntries() throws IOException {
        Files.writeString(folder.resolve(UUID.randomUUID() + ".yml"), "owner: x\n");

        assertTrue(new YamlRemovedNpcStore(folder, Logger.getAnonymousLogger()).readAll().isEmpty());
    }

    @Test
    void aMissingFolderHasNoEntries() throws IOException {
        assertEquals(0, new YamlRemovedNpcStore(folder.resolve("absent"), Logger.getAnonymousLogger()).readAll().size());
    }
}
