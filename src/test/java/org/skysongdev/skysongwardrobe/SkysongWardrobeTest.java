package org.skysongdev.skysongwardrobe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkysongWardrobeTest {
    private ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void pluginEnables() {
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);

        assertTrue(plugin.isEnabled());
    }

    @Test
    void pluginDisablesCleanly() {
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());
    }

    @Test
    void pluginYmlDeclaresItsDependencies() {
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);

        assertEquals(List.of("Skysong_Items"), plugin.getPluginMeta().getPluginDependencies());
        assertEquals(List.of("SkysongStats"), plugin.getPluginMeta().getPluginSoftDependencies());
    }
}
