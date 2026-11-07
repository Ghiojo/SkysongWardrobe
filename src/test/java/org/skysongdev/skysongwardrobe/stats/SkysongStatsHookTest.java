package org.skysongdev.skysongwardrobe.stats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;
import org.skysongdev.skysongstats.api.ItemSource;
import org.skysongdev.skysongstats.api.ItemSources;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeUpdateEvent;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SkysongStatsHookTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private final ItemSources itemSources = mock(ItemSources.class);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void withoutSkysongStatsNothingIsRegisteredAndUpdatesStillWork() {
        server.getPluginManager().callEvent(new WardrobeUpdateEvent(server.addPlayer()));

        verify(itemSources, never()).recount(any());
    }

    @Test
    void registersTheWornItemsAsTheWardrobeSource() {
        register();

        ArgumentCaptor<ItemSource> source = ArgumentCaptor.forClass(ItemSource.class);
        verify(itemSources).register(eq(plugin), eq(SkysongStatsHook.SOURCE_ID), source.capture());
        assertInstanceOf(WornItemSource.class, source.getValue());
    }

    @Test
    void aWardrobeChangeAsksForARecount() {
        // Joining fires a wardrobe update too, so the player joins before the hook is in.
        PlayerMock player = server.addPlayer();
        register();

        server.getPluginManager().callEvent(new WardrobeUpdateEvent(player));

        verify(itemSources).recount(player);
    }

    private void register() {
        WardrobeRepository wardrobes = new WardrobeRepository(Path.of("unused"), Logger.getAnonymousLogger());
        SkysongStatsHook.register(plugin, itemSources, wardrobes, plugin.configuration());
    }
}
