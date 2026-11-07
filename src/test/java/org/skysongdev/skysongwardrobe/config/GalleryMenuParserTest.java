package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryMenuParserTest {
    private final GalleryMenuParser parser = new GalleryMenuParser();

    @BeforeEach
    void startServer() {
        // Material.matchMaterial needs a registry, which MockBukkit provides.
        MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void missingSectionUsesDefaults() throws InvalidConfigurationException {
        assertEquals(GalleryMenuParser.DEFAULTS, parse("title: x\n"));
        assertTrue(parser.warnings().isEmpty());
    }

    @Test
    void readsEveryValue() throws InvalidConfigurationException {
        GalleryMenuLayout layout = parse("""
                gallery:
                  title: "<gold><player>"
                  filler: {material: BLACK_STAINED_GLASS_PANE, name: "-"}
                  previous-page: {material: SPECTRAL_ARROW, name: "Back"}
                  next-page: {material: TIPPED_ARROW, name: "On"}
                """);

        assertEquals(new GalleryMenuLayout("<gold><player>",
                new MenuItem(Material.BLACK_STAINED_GLASS_PANE, "-"),
                new MenuItem(Material.SPECTRAL_ARROW, "Back"),
                new MenuItem(Material.TIPPED_ARROW, "On")), layout);
    }

    @Test
    void badMaterialFallsBackWithAWarning() throws InvalidConfigurationException {
        GalleryMenuLayout layout = parse("gallery:\n  next-page: {material: NOPE}\n");

        assertEquals(GalleryMenuParser.DEFAULTS.next(), layout.next());
        assertEquals(List.of("gallery.next-page.material NOPE is not an item, so ARROW is used."), parser.warnings());
    }

    private GalleryMenuLayout parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return parser.parse(file);
    }
}
