package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.items.LayerKind;

import java.util.List;
import java.util.Optional;

/**
 * What one worn item asks to draw: its layer tags and dye colour, read from the item. SkysongItems
 * only lets its tags be read on the main thread, so renders read these first and hand them to
 * the off-thread work, which never touches an item.
 *
 * @param dyable  the {@code wardrobe_dyable_layer} value, if any
 * @param dye     the tint for the dyable layer
 * @param regular the {@code wardrobe_layer} value, if any
 */
public record ItemLayers(Optional<String> dyable, Color dye, Optional<String> regular) {
    /** Reads every worn item's layers, in order. Main thread only. */
    public static List<ItemLayers> read(ItemTagGateway tags, List<ItemStack> worn) {
        return worn.stream()
                .map(item -> new ItemLayers(tags.layer(item, LayerKind.DYABLE), tags.dyeColor(item),
                        tags.layer(item, LayerKind.REGULAR)))
                .toList();
    }
}
