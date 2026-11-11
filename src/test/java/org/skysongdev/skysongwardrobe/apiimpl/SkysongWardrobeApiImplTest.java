package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkysongWardrobeApiImplTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getFailsWithAClearMessageBeforeSkysongWardrobeIsEnabled() {
        IllegalStateException error = assertThrows(IllegalStateException.class, SkysongWardrobeApi::get);

        assertTrue(error.getMessage().contains("depend: [SkysongWardrobe]"), error.getMessage());
    }

    @Test
    void enablingThePluginRegistersTheApiAsAService() {
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);

        RegisteredServiceProvider<SkysongWardrobeApi> registration =
                server.getServicesManager().getRegistration(SkysongWardrobeApi.class);

        assertInstanceOf(SkysongWardrobeApiImpl.class, registration.getProvider());
        assertSame(plugin, registration.getPlugin());
        assertSame(registration.getProvider(), SkysongWardrobeApi.get());
        assertSame(registration.getProvider(), server.getServicesManager().load(SkysongWardrobeApi.class));
    }

    @Test
    void apiVersionIsTheImplementedVersion() {
        MockBukkit.load(SkysongWardrobe.class);

        assertEquals(SkysongWardrobeApiImpl.API_VERSION, SkysongWardrobeApi.get().apiVersion());
    }

    @Test
    void disablingThePluginRemovesTheApi() {
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(server.getServicesManager().isProvidedFor(SkysongWardrobeApi.class));
        assertThrows(IllegalStateException.class, SkysongWardrobeApi::get);
    }
}
