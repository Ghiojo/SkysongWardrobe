package org.skysongdev.skysongwardrobe.storage.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.npc.RemovedNpc;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStore;
import org.skysongdev.skysongwardrobe.npc.YamlRemovedNpcStore;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemovedNpcImporterTest {
    private static final Instant MONDAY = Instant.parse("2026-11-02T10:00:00Z");

    @TempDir
    Path folder;

    private static RemovedNpc entry(String name) {
        return new RemovedNpc(UUID.randomUUID(), UUID.randomUUID(), 7, name, MONDAY);
    }

    @Test
    void copiesEveryEntryTheTargetLacksAndRunsTwiceSafely() throws IOException {
        RemovedNpcStore files = new YamlRemovedNpcStore(folder.resolve("files"), Logger.getAnonymousLogger());
        RemovedNpcStore target = new YamlRemovedNpcStore(folder.resolve("target"), Logger.getAnonymousLogger());
        RemovedNpc baker = entry("Baker");
        files.write(baker);
        files.write(entry("Guard"));
        RemovedNpc kept = new RemovedNpc(baker.entry(), baker.npc(), 7, "Kept", MONDAY);
        target.write(kept);

        assertEquals(new ImportResult("removed npcs", 1, 1), new RemovedNpcImporter(files, target).run());
        assertEquals(new ImportResult("removed npcs", 0, 2), new RemovedNpcImporter(files, target).run());
        assertEquals(kept, target.readAll().stream().filter(e -> e.entry().equals(baker.entry())).findFirst().orElseThrow());
    }
}
