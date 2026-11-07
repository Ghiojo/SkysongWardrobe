package org.skysongdev.skysongwardrobe.items;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongitems.api.ItemTags;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongitems.api.TagType;
import org.skysongdev.skysongitems.api.event.SkysongItemsReadyEvent;
import org.skysongdev.skysongitems.api.event.SkysongItemsRegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagRegistrationListenerTest {
    private final Plugin plugin = mock(Plugin.class);
    private final SkysongItemsApi api = mock(SkysongItemsApi.class);
    private final ItemTags tags = mock(ItemTags.class);
    private final SkysongItemsRegisterEvent.Registrar registrar = mock(SkysongItemsRegisterEvent.Registrar.class);
    private final List<String> warnings = new ArrayList<>();
    private final TagRegistrationListener listener = new TagRegistrationListener(plugin, () -> api);

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        when(plugin.getLogger()).thenReturn(logger);
        when(api.tags()).thenReturn(tags);
        when(registrar.registerTag(any(), any(), any())).thenReturn(true);
    }

    @Test
    void registersEveryStringTag() {
        listener.onRegister(new SkysongItemsRegisterEvent(registrar));

        verify(registrar).registerTag(plugin, "wardrobe_layer", TagType.STRING);
        verify(registrar).registerTag(plugin, "wardrobe_dyable_layer", TagType.STRING);
        verify(registrar).registerTag(plugin, "wardrobe_type", TagType.STRING);
        verify(registrar).registerTag(plugin, "mannequin_skin", TagType.STRING);
        verify(registrar).registerTag(plugin, "mannequin_model", TagType.STRING);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void registersAgainOnEveryReload() {
        listener.onRegister(new SkysongItemsRegisterEvent(registrar));
        listener.onRegister(new SkysongItemsRegisterEvent(registrar));

        verify(registrar, times(2)).registerTag(plugin, "wardrobe_layer", TagType.STRING);
    }

    @Test
    void warnsWhenATagIsRefused() {
        when(registrar.registerTag(plugin, "wardrobe_type", TagType.STRING)).thenReturn(false);

        listener.onRegister(new SkysongItemsRegisterEvent(registrar));

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("wardrobe_type"));
    }

    @Test
    void quietWhenDyeColorIsAnIntTag() {
        when(tags.type(eq("dye_color"))).thenReturn(Optional.of(TagType.INT));

        listener.onReady(new SkysongItemsReadyEvent());

        assertTrue(warnings.isEmpty());
    }

    @Test
    void warnsWhenDyeColorIsMissingOrNotAnInt() {
        when(tags.type(eq("dye_color"))).thenReturn(Optional.empty());
        listener.onReady(new SkysongItemsReadyEvent());

        when(tags.type(eq("dye_color"))).thenReturn(Optional.of(TagType.STRING));
        listener.onReady(new SkysongItemsReadyEvent());

        assertEquals(2, warnings.size());
        assertTrue(warnings.getFirst().contains("dyable layers will not be tinted"));
    }
}
