package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlTextureCacheTest extends TextureCacheContract {
    @TempDir
    Path folder;

    @Override
    TextureCache emptyCache() {
        return YamlTextureCache.load(folder.resolve("texture-cache.yml"));
    }

    @Test
    void storedTexturesSurviveAReload() throws IOException {
        Path file = folder.resolve("texture-cache.yml");
        YamlTextureCache.load(file).put("abc", SIGNED);

        YamlTextureCache reloaded = YamlTextureCache.load(file);

        assertEquals(Optional.of(SIGNED), reloaded.find("abc"));
        assertTrue(reloaded.isOwn(SIGNED));
        assertFalse(reloaded.isOwn(new SignedTexture("mojang", "sig")));
    }

    @Test
    void missingFileIsAnEmptyCache() {
        assertEquals(Optional.empty(), YamlTextureCache.load(folder.resolve("none.yml")).find("abc"));
    }

    @Test
    void entriesMissingASignatureAreSkipped() throws IOException {
        Path file = folder.resolve("texture-cache.yml");
        Files.writeString(file, "good:\n  value: v\n  signature: s\nbroken:\n  value: v2\n");

        YamlTextureCache cache = YamlTextureCache.load(file);

        assertEquals(Optional.of(new SignedTexture("v", "s")), cache.find("good"));
        assertEquals(Optional.empty(), cache.find("broken"));
        assertFalse(cache.isOwn(new SignedTexture("v2", "")));
    }

    @Test
    void theFileIsCreatedInAMissingFolder() throws IOException {
        Path file = folder.resolve("nested").resolve("texture-cache.yml");

        YamlTextureCache.load(file).put("abc", SIGNED);

        assertTrue(Files.isRegularFile(file));
    }
}
