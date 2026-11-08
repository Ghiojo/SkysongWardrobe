package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlWardrobeStoreTest extends WardrobeStoreContract {
    private static final UUID OWNER = UUID.randomUUID();

    @TempDir
    Path folder;

    @Override
    protected WardrobeStore emptyStore() {
        return new YamlWardrobeStore(folder, Logger.getAnonymousLogger());
    }

    @Test
    void keepingAsideRenamesTheFile() throws IOException {
        YamlWardrobeStore store = new YamlWardrobeStore(folder, Logger.getAnonymousLogger());
        store.write(OWNER, Map.of("coat", new byte[]{1, 2, 3}));

        store.delete(OWNER, true);

        assertTrue(Files.notExists(folder.resolve(OWNER + ".yml")));
        assertTrue(Files.isRegularFile(folder.resolve(OWNER + ".broken.yml")));
    }

    @Test
    void textThatIsNotBase64StaysInTheFileAndKeepsItAside() throws IOException {
        Files.writeString(folder.resolve(OWNER + ".yml"), "owner: " + OWNER + "\nslots:\n  coat: \"not base64!\"\n");
        YamlWardrobeStore store = new YamlWardrobeStore(folder, Logger.getAnonymousLogger());

        assertTrue(store.read(OWNER).isEmpty());
        store.write(OWNER, Map.of("boots", new byte[]{1}));
        assertEquals("not base64!", YamlConfiguration.loadConfiguration(folder.resolve(OWNER + ".yml").toFile())
                .getString("slots.coat"));

        store.delete(OWNER, false);
        assertTrue(Files.isRegularFile(folder.resolve(OWNER + ".broken.yml")));
    }
}
