package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeUpdateEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RenderTriggerTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private final SkinRenderer renderer = mock(SkinRenderer.class);
    private final BaseSkins bases = mock(BaseSkins.class);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        server.getPluginManager().registerEvents(new RenderTrigger(renderer, bases), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void anUpdateRendersThePlayer() {
        PlayerMock player = server.addPlayer();

        server.getPluginManager().callEvent(new WardrobeUpdateEvent(player));

        verify(renderer, atLeastOnce()).render(player);
    }

    @Test
    void quittingForgetsTheBaseSkin() {
        PlayerMock player = server.addPlayer();

        player.disconnect();

        verify(bases).forget(player.getUniqueId());
    }

    @Test
    void thePluginStartsTheRenderQueue() {
        assertNotNull(plugin.renderQueue());
    }
}
