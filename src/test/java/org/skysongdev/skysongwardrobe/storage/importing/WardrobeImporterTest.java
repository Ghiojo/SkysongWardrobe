package org.skysongdev.skysongwardrobe.storage.importing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.storage.DatabaseSettings;
import org.skysongdev.skysongwardrobe.storage.StorageBackend;
import org.skysongdev.skysongwardrobe.storage.StorageSettings;
import org.skysongdev.skysongwardrobe.storage.StorageStartup;
import org.skysongdev.skysongwardrobe.storage.StorageType;
import org.skysongdev.skysongwardrobe.storage.YamlStorage;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeStore;
import org.skysongdev.skysongwardrobe.wardrobe.YamlWardrobeStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WardrobeImporterTest {
    private static final UUID ALEX = UUID.randomUUID();
    private static final UUID SAM = UUID.randomUUID();

    @TempDir
    Path folder;

    private StorageBackend database;
    private Path players;

    @BeforeEach
    void setUp() throws IOException {
        players = YamlStorage.wardrobeFolder(folder, WardrobeKind.PLAYER);
        YamlWardrobeStore files = new YamlWardrobeStore(players, Logger.getAnonymousLogger());
        files.write(ALEX, Map.of("coat", new byte[]{1}));
        files.write(SAM, Map.of("boots", new byte[]{2}));
        StorageSettings sqlite = new StorageSettings(StorageType.SQLITE, "data.db",
                new DatabaseSettings("localhost", 3306, "skysong", "root", "", "sw_", 4));
        database = StorageStartup.open(() -> sqlite, folder, Clock.systemUTC(), Logger.getAnonymousLogger()).orElseThrow();
    }

    private WardrobeImporter importer() {
        return new WardrobeImporter(WardrobeKind.PLAYER, players, database.wardrobes(WardrobeKind.PLAYER),
                Logger.getAnonymousLogger());
    }

    @Test
    void copiesEveryWardrobeTheDatabaseLacks() throws IOException {
        WardrobeStore target = database.wardrobes(WardrobeKind.PLAYER);
        target.write(SAM, Map.of("boots", new byte[]{9}));

        ImportResult result = importer().run();

        assertEquals(new ImportResult("player wardrobes", 1, 1), result);
        assertArrayEquals(new byte[]{1}, target.read(ALEX).get("coat"));
        assertArrayEquals(new byte[]{9}, target.read(SAM).get("boots"));
    }

    @Test
    void runningItTwiceAddsNothingAndLeavesTheFilesAlone() throws IOException {
        String before = Files.readString(players.resolve(ALEX + ".yml"));
        importer().run();

        assertEquals(new ImportResult("player wardrobes", 0, 2), importer().run());
        assertEquals(before, Files.readString(players.resolve(ALEX + ".yml")));
    }

    @Test
    void brokenAndForeignFilesAreLeftOut() throws IOException {
        Files.writeString(players.resolve(UUID.randomUUID() + ".broken.yml"), "slots: {}\n");
        Files.writeString(players.resolve("notes.yml"), "hello: there\n");

        assertEquals(new ImportResult("player wardrobes", 2, 0), importer().run());
    }

    @Test
    void aMissingFolderImportsNothing() throws IOException {
        assertEquals(new ImportResult("npc wardrobes", 0, 0), new WardrobeImporter(WardrobeKind.NPC,
                folder.resolve("none"), database.wardrobes(WardrobeKind.NPC), Logger.getAnonymousLogger()).run());
    }

    @Test
    void theDatabaseBackendImportsTexturesAndEveryWardrobeKind() {
        List<String> datasets = database.importers(folder).stream().map(DatasetImporter::dataset).toList();

        assertEquals(List.of("textures", "player wardrobes", "mannequin wardrobes", "npc wardrobes", "mannequins", "npc records"),
                datasets);
    }
}
