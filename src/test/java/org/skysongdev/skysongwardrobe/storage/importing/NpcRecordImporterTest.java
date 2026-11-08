package org.skysongdev.skysongwardrobe.storage.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.npc.NpcRecord;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStore;
import org.skysongdev.skysongwardrobe.npc.YamlNpcRecordStore;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcRecordImporterTest {
    @TempDir
    Path folder;

    private static NpcRecord record(String value) {
        return new NpcRecord(UUID.randomUUID(), new SignedTexture(value, value + "-signature"));
    }

    @Test
    void copiesEveryRecordTheTargetLacksAndRunsTwiceSafely() throws IOException {
        Path files = folder.resolve("npcs");
        NpcRecordStore source = new YamlNpcRecordStore(files, Logger.getAnonymousLogger());
        NpcRecordStore target = new YamlNpcRecordStore(folder.resolve("target"), Logger.getAnonymousLogger());
        NpcRecord guard = record("guard");
        source.write(guard);
        source.write(record("baker"));
        target.write(guard.withBase(new SignedTexture("kept", "kept-signature")));

        assertEquals(new ImportResult("npc records", 1, 1), importer(files, target).run());
        assertEquals(new ImportResult("npc records", 0, 2), importer(files, target).run());
        assertEquals(Optional.of(new SignedTexture("kept", "kept-signature")), target.read(guard.id()).map(NpcRecord::base));
    }

    @Test
    void otherFilesAndFoldersAreNotRecords() throws IOException {
        Path files = folder.resolve("npcs");
        Files.createDirectories(files.resolve("wardrobes"));
        Files.writeString(files.resolve("wardrobes").resolve(UUID.randomUUID() + ".yml"), "owner: x\n");
        Files.writeString(files.resolve("notes.yml"), "hello: world\n");
        NpcRecordStore target = new YamlNpcRecordStore(folder.resolve("target"), Logger.getAnonymousLogger());

        assertEquals(new ImportResult("npc records", 0, 0), importer(files, target).run());
    }

    @Test
    void aMissingFolderImportsNothing() throws IOException {
        NpcRecordStore target = new YamlNpcRecordStore(folder.resolve("target"), Logger.getAnonymousLogger());

        assertEquals(new ImportResult("npc records", 0, 0), importer(folder.resolve("absent"), target).run());
    }

    private static NpcRecordImporter importer(Path files, NpcRecordStore target) {
        return new NpcRecordImporter(files, target, Logger.getAnonymousLogger());
    }
}
