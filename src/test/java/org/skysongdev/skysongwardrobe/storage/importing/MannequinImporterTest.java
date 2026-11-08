package org.skysongdev.skysongwardrobe.storage.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.mannequin.MannequinStore;
import org.skysongdev.skysongwardrobe.mannequin.YamlMannequinStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MannequinImporterTest {
    @TempDir
    Path folder;

    private static MannequinStore.Row row(String skin) {
        return new MannequinStore.Row(UUID.randomUUID(), UUID.randomUUID(), skin, "WIDE", new byte[]{1});
    }

    @Test
    void copiesEveryMannequinTheTargetLacksAndRunsTwiceSafely() throws IOException {
        MannequinStore files = new YamlMannequinStore(folder.resolve("files"), Logger.getAnonymousLogger());
        MannequinStore target = new YamlMannequinStore(folder.resolve("target"), Logger.getAnonymousLogger());
        MannequinStore.Row oak = row("oak");
        files.write(oak);
        files.write(row("birch"));
        target.write(new MannequinStore.Row(oak.id(), oak.owner(), "kept", "WIDE", new byte[]{9}));

        assertEquals(new ImportResult("mannequins", 1, 1), new MannequinImporter(files, target).run());
        assertEquals(new ImportResult("mannequins", 0, 2), new MannequinImporter(files, target).run());
        assertEquals("kept", target.readAll().rows().stream().filter(row -> row.id().equals(oak.id()))
                .findFirst().orElseThrow().skin());
    }
}
