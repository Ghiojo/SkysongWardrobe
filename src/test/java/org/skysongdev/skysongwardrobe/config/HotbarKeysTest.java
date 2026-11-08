package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotbarKeysTest {
    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    @Test
    void aSectionCoversEveryKeyUnderIt() {
        HotbarKeys keys = new HotbarKeys(List.of("wardrobe"));

        assertTrue(keys.contains("wardrobe.closed"));
        assertTrue(keys.contains("wardrobe.refused.slot-full"));
        assertTrue(keys.contains("wardrobe"));
    }

    @Test
    void aSectionDoesNotCoverKeysThatOnlyStartWithItsName() {
        assertFalse(new HotbarKeys(List.of("apply")).contains("applyx.done"));
    }

    @Test
    void aSingleKeyCoversOnlyItself() {
        HotbarKeys keys = new HotbarKeys(List.of("upload.saved"));

        assertTrue(keys.contains("upload.saved"));
        assertFalse(keys.contains("upload.error"));
    }

    @Test
    void theFilesListWinsOverTheBundledOne() throws InvalidConfigurationException {
        HotbarKeys keys = HotbarKeys.from(yaml("hotbar: [render]"), yaml("hotbar: [wardrobe]"));

        assertTrue(keys.contains("render.queued"));
        assertFalse(keys.contains("wardrobe.closed"));
    }

    @Test
    void anEmptyListInTheFileSendsEverythingToChat() throws InvalidConfigurationException {
        HotbarKeys keys = HotbarKeys.from(yaml("hotbar: []"), yaml("hotbar: [wardrobe]"));

        assertFalse(keys.contains("wardrobe.closed"));
    }

    @Test
    void aFileWithoutTheListUsesTheBundledOne() throws InvalidConfigurationException {
        HotbarKeys keys = HotbarKeys.from(yaml("prefix: x"), yaml("hotbar: [wardrobe]"));

        assertTrue(keys.contains("wardrobe.closed"));
    }
}
