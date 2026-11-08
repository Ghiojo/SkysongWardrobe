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

class YamlNpcRecordStoreTest extends NpcRecordStoreContract {
    @TempDir
    Path folder;

    @Override
    protected NpcRecordStore emptyStore() {
        return new YamlNpcRecordStore(folder, Logger.getAnonymousLogger());
    }

    @Test
    void aFileWithoutABaseSkinIsMissingAndLeftAlone() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = folder.resolve(id + ".yml");
        Files.writeString(file, "base:\n  value: only-half\n");

        assertTrue(new YamlNpcRecordStore(folder, Logger.getAnonymousLogger()).read(id).isEmpty());
        assertEquals("base:\n  value: only-half\n", Files.readString(file));
    }
}
