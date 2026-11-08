package org.skysongdev.skysongwardrobe.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsParserTest {
    private final SettingsParser parser = new SettingsParser();

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    @Test
    void emptyConfigUsesEveryDefaultSilently() {
        WardrobeSettings settings = parser.parse(new YamlConfiguration());

        assertFalse(settings.verbose());
        assertEquals(List.of("skysongwardrobe.gallery"), settings.galleryPermissions());
        assertEquals(512, settings.maxDownloadKb());
        assertEquals(10, settings.downloadTimeoutSeconds());
        assertEquals(List.of("skysongwardrobe.apply"), settings.applyPermissions());
        assertEquals(30, settings.uploadCooldownSeconds());
        assertEquals("", settings.mineSkinApiKey());
        assertFalse(settings.hasMineSkinApiKey());
        assertEquals(SkinVisibility.UNLISTED, settings.skinVisibility());
        assertEquals(new MannequinSettings(List.of("skysongwardrobe.mannequin"), 0, 15), settings.mannequins());
        assertEquals(List.of("skysongwardrobe.npc"), settings.npcPermissions());
        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void readsEveryValue() throws InvalidConfigurationException {
        WardrobeSettings settings = parser.parse(yaml("""
                verbose: true
                gallery:
                  permissions: [skysongstats.tailor, skysongwardrobe.gallery]
                  max-download-kb: 256
                  download-timeout-seconds: 5
                apply:
                  permissions: [skysongstats.dyer]
                render:
                  upload-cooldown-seconds: 0
                mineskin:
                  api-key: " abc123 "
                  visibility: Private
                mannequins:
                  permissions: [skysongstats.carpenter]
                  max-per-player: 3
                  max-per-chunk: 0
                npcs:
                  permissions: [skysongeconomy.shopkeeper]
                """));

        assertTrue(settings.verbose());
        assertEquals(List.of("skysongstats.tailor", "skysongwardrobe.gallery"), settings.galleryPermissions());
        assertEquals(256, settings.maxDownloadKb());
        assertEquals(5, settings.downloadTimeoutSeconds());
        assertEquals(List.of("skysongstats.dyer"), settings.applyPermissions());
        assertEquals(0, settings.uploadCooldownSeconds());
        assertEquals("abc123", settings.mineSkinApiKey());
        assertTrue(settings.hasMineSkinApiKey());
        assertEquals(SkinVisibility.PRIVATE, settings.skinVisibility());
        assertEquals(new MannequinSettings(List.of("skysongstats.carpenter"), 3, 0), settings.mannequins());
        assertEquals(List.of("skysongeconomy.shopkeeper"), settings.npcPermissions());
        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void emptyPermissionListFallsBackWithAWarning() throws InvalidConfigurationException {
        WardrobeSettings settings = parser.parse(yaml("""
                gallery:
                  permissions: []
                apply:
                  permissions: ["  "]
                """));

        assertEquals(List.of("skysongwardrobe.gallery"), settings.galleryPermissions());
        assertEquals(List.of("skysongwardrobe.apply"), settings.applyPermissions());
        assertEquals(2, parser.warnings().size());
        assertTrue(parser.warnings().get(0).startsWith("gallery.permissions"));
    }

    @Test
    void invalidNumbersFallBackWithAWarning() throws InvalidConfigurationException {
        WardrobeSettings settings = parser.parse(yaml("""
                gallery:
                  max-download-kb: 0
                  download-timeout-seconds: soon
                render:
                  upload-cooldown-seconds: -5
                """));

        assertEquals(512, settings.maxDownloadKb());
        assertEquals(10, settings.downloadTimeoutSeconds());
        assertEquals(30, settings.uploadCooldownSeconds());
        assertEquals(3, parser.warnings().size());
    }

    @Test
    void invalidMannequinLimitsFallBackWithAWarning() throws InvalidConfigurationException {
        WardrobeSettings settings = parser.parse(yaml("""
                mannequins:
                  max-per-player: -1
                  max-per-chunk: many
                """));

        assertEquals(0, settings.mannequins().perPlayer());
        assertEquals(15, settings.mannequins().perChunk());
        assertEquals(2, parser.warnings().size());
        assertTrue(parser.warnings().get(1).startsWith("mannequins.max-per-chunk"));
    }

    @Test
    void publicOrUnknownVisibilityFallsBackToUnlistedWithAWarning() throws InvalidConfigurationException {
        for (String value : List.of("public", "secret")) {
            WardrobeSettings settings = parser.parse(yaml("mineskin: {visibility: " + value + "}"));

            assertEquals(SkinVisibility.UNLISTED, settings.skinVisibility());
            assertEquals(List.of("mineskin.visibility must be unlisted or private, so it uses unlisted."),
                    parser.warnings());
        }
    }

    @Test
    void zeroMeansNoLimit() {
        assertTrue(MannequinSettings.withinLimit(0, 500));
        assertTrue(MannequinSettings.withinLimit(15, 14));
        assertFalse(MannequinSettings.withinLimit(15, 15));
    }

    @Test
    void warningsOnlyDescribeTheLastParse() throws InvalidConfigurationException {
        parser.parse(yaml("gallery: {max-download-kb: 0}"));

        parser.parse(new YamlConfiguration());

        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void permissionListsCannotBeChangedAfterParsing() {
        WardrobeSettings settings = parser.parse(new YamlConfiguration());

        assertThrows(UnsupportedOperationException.class,
                () -> settings.galleryPermissions().add("anything"));
    }
}
