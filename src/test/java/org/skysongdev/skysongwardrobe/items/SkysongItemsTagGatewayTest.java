package org.skysongdev.skysongwardrobe.items;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongitems.api.ItemTags;
import org.skysongdev.skysongitems.api.ItemTemplates;
import org.skysongdev.skysongitems.api.SkysongItemsApi;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkysongItemsTagGatewayTest {
    private final SkysongItemsApi api = mock(SkysongItemsApi.class);
    private final ItemTags tags = mock(ItemTags.class);
    private final ItemTemplates templates = mock(ItemTemplates.class);
    private final ItemStack item = mock(ItemStack.class);
    private final Player player = mock(Player.class);
    private final SkysongItemsTagGateway gateway = new SkysongItemsTagGateway(api);

    @BeforeEach
    void setUp() {
        when(api.tags()).thenReturn(tags);
        when(api.templates()).thenReturn(templates);
        when(tags.getString(item, "wardrobe_layer")).thenReturn(Optional.empty());
        when(tags.getString(item, "wardrobe_dyable_layer")).thenReturn(Optional.empty());
        when(tags.getString(item, "wardrobe_type")).thenReturn(Optional.empty());
        when(tags.getInt(item, "dye_color")).thenReturn(OptionalInt.empty());
    }

    @Test
    void readsEachLayerFromItsOwnTag() {
        when(tags.getString(item, "wardrobe_layer")).thenReturn(Optional.of("uuid:cape"));
        when(tags.getString(item, "wardrobe_dyable_layer")).thenReturn(Optional.of("uuid:cape_trim"));

        assertEquals(Optional.of("uuid:cape"), gateway.layer(item, LayerKind.REGULAR));
        assertEquals(Optional.of("uuid:cape_trim"), gateway.layer(item, LayerKind.DYABLE));
    }

    @Test
    void blankValuesCountAsMissing() {
        when(tags.getString(item, "wardrobe_layer")).thenReturn(Optional.of("  "));

        assertEquals(Optional.empty(), gateway.layer(item, LayerKind.REGULAR));
        assertEquals(Optional.empty(), gateway.type(item));
    }

    @Test
    void typeIsLowercased() {
        when(tags.getString(item, "wardrobe_type")).thenReturn(Optional.of("Coat"));

        assertEquals(Optional.of("coat"), gateway.type(item));
    }

    @Test
    void dyeColorIsPackedRgbAndDefaultsToWhite() {
        assertEquals(Color.WHITE, gateway.dyeColor(item));

        when(tags.getInt(item, "dye_color")).thenReturn(OptionalInt.of(0x3366CC));
        assertEquals(Color.fromRGB(0x33, 0x66, 0xCC), gateway.dyeColor(item));

        // An ARGB-style value keeps only its RGB bits.
        when(tags.getInt(item, "dye_color")).thenReturn(OptionalInt.of(0xFF3366CC));
        assertEquals(Color.fromRGB(0x33, 0x66, 0xCC), gateway.dyeColor(item));
    }

    @Test
    void writesAndRemovesThroughSkysongItems() {
        when(tags.remove(player, item, "wardrobe_dyable_layer")).thenReturn(true);

        gateway.setLayer(player, item, LayerKind.REGULAR, "uuid:cape");
        boolean removed = gateway.removeLayer(player, item, LayerKind.DYABLE);

        verify(tags).set(player, item, "wardrobe_layer", "uuid:cape");
        assertTrue(removed);
    }

    @Test
    void onlySkysongItemsCount() {
        when(templates.isSkysongItem(item)).thenReturn(true);

        assertTrue(gateway.isSkysongItem(item));
        assertFalse(gateway.isSkysongItem(mock(ItemStack.class)));
        assertFalse(gateway.isSkysongItem(null));
    }
}
