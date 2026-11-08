package org.skysongdev.skysongwardrobe.mannequin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlMannequinStoreTest extends MannequinStoreContract {
    @TempDir
    Path folder;

    @Override
    protected MannequinStore emptyStore() {
        return new YamlMannequinStore(folder, Logger.getAnonymousLogger());
    }

    @Test
    void aBrokenFileIsUnreadableAndLeftAlone() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = folder.resolve(id + ".yml");
        Files.writeString(file, "owner: nobody\n");

        MannequinStore.Contents contents = new YamlMannequinStore(folder, Logger.getAnonymousLogger()).readAll();

        assertTrue(contents.rows().isEmpty());
        assertEquals(Set.of(id), contents.unreadable());
        assertEquals("owner: nobody\n", Files.readString(file));
    }
}
