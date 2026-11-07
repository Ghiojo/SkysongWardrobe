package org.skysongdev.skysongwardrobe.config;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigurationTest {
    private SkysongWardrobe plugin;

    @BeforeEach
    void loadPlugin() {
        MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void firstStartWritesBothDefaultFiles() {
        assertTrue(new File(plugin.getDataFolder(), "config.yml").isFile());
        assertTrue(new File(plugin.getDataFolder(), "messages.yml").isFile());
        assertTrue(new File(plugin.getDataFolder(), "wardrobe.yml").isFile());
    }

    @Test
    void bundledLayoutHasCoatGlovesAndBootsInTheSecondColumn() {
        WardrobeLayout layout = plugin.configuration().layout().orElseThrow();

        assertEquals(List.of("boots", "gloves", "coat"), layout.slots().stream().map(SlotDefinition::id).toList());
        assertEquals(List.of(28, 19, 10), layout.slots().stream().map(SlotDefinition::position).toList());
    }

    @Test
    void brokenLayoutClosesTheWardrobeUntilFixed() throws IOException {
        File layout = new File(plugin.getDataFolder(), "wardrobe.yml");
        Files.writeString(layout.toPath(), "slots:\n  coat: {position: 99, accepts: [coat], priority: 1}\n");
        plugin.configuration().reload();
        assertTrue(plugin.configuration().layout().isEmpty());

        Files.writeString(layout.toPath(), "slots:\n  coat: {position: 9, accepts: [coat], priority: 1}\n");
        plugin.configuration().reload();
        assertEquals(9, plugin.configuration().layout().orElseThrow().slots().getFirst().position());
    }

    @Test
    void bundledConfigMatchesTheDefaults() {
        WardrobeSettings settings = plugin.configuration().settings();

        assertEquals(List.of("skysongwardrobe.gallery"), settings.galleryPermissions());
        assertEquals(30, settings.uploadCooldownSeconds());
    }

    @Test
    void reloadPicksUpEditedFiles() throws IOException {
        File config = new File(plugin.getDataFolder(), "config.yml");
        Files.writeString(config.toPath(), "verbose: true\nmineskin:\n  api-key: key\n");
        File messages = new File(plugin.getDataFolder(), "messages.yml");
        Files.writeString(messages.toPath(), "prefix: \"[Changed]\"\n");

        plugin.configuration().reload();

        assertTrue(plugin.configuration().settings().verbose());
        assertTrue(plugin.configuration().settings().hasMineSkinApiKey());
        assertEquals("[Changed]", PlainTextComponentSerializer.plainText()
                .serialize(plugin.configuration().messages().get("prefix")));
    }

    @Test
    void reloadKeepsAnEditedFile() throws IOException {
        File config = new File(plugin.getDataFolder(), "config.yml");
        Files.writeString(config.toPath(), "verbose: true\n");

        plugin.configuration().reload();

        assertEquals("verbose: true\n", Files.readString(config.toPath()));
    }
}
