package org.skysongdev.skysongwardrobe.storage.importing;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;
import org.skysongdev.skysongwardrobe.storage.StorageBackend;
import org.skysongdev.skysongwardrobe.storage.StorageSettings;
import org.skysongdev.skysongwardrobe.storage.StorageSettingsParser;
import org.skysongdev.skysongwardrobe.storage.StorageStartup;
import org.skysongdev.skysongwardrobe.storage.StorageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureImporterTest {
    private static final SignedTexture ONE = new SignedTexture("v1", "s1");
    private static final SignedTexture TWO = new SignedTexture("v2", "s2");

    @TempDir
    Path folder;

    private Path yaml;
    private StorageBackend database;

    @BeforeEach
    void setUp() throws Exception {
        yaml = folder.resolve("texture-cache.yml");
        YamlTextureCache files = YamlTextureCache.load(yaml);
        files.put("one", ONE);
        files.put("two", TWO);
        StorageSettings sqlite = new StorageSettings(StorageType.SQLITE, "data.db",
                new StorageSettingsParser().parse(new YamlConfiguration()).mysql());
        database = StorageStartup.open(() -> sqlite, folder, Clock.systemUTC(), Logger.getAnonymousLogger()).orElseThrow();
    }

    @Test
    void copiesEverySkinTheDatabaseLacks() throws IOException {
        TextureCache target = database.textures();
        target.put("two", new SignedTexture("kept", "kept"));

        ImportResult result = new TextureImporter(yaml, target).run();

        assertEquals(new ImportResult("textures", 1, 1), result);
        assertEquals(Optional.of(ONE), target.find("one"));
        assertEquals(Optional.of(new SignedTexture("kept", "kept")), target.find("two"));
    }

    @Test
    void runningItTwiceAddsNothingAndLeavesTheFileAlone() throws IOException {
        String before = Files.readString(yaml);
        new TextureImporter(yaml, database.textures()).run();

        ImportResult again = new TextureImporter(yaml, database.textures()).run();

        assertEquals(new ImportResult("textures", 0, 2), again);
        assertEquals(before, Files.readString(yaml));
    }

    @Test
    void aMissingFileImportsNothing() throws IOException {
        assertEquals(new ImportResult("textures", 0, 0),
                new TextureImporter(folder.resolve("none.yml"), database.textures()).run());
    }

    @Test
    void theDatabaseBackendOffersTheTextureImport() {
        assertEquals("textures", database.importers(folder).getFirst().dataset());
    }
}
