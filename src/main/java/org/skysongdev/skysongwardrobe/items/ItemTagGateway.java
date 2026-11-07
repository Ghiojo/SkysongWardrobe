package org.skysongdev.skysongwardrobe.items;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Everything SkysongWardrobe needs from an item's SkysongItems tags. The rest of the plugin
 * goes through this instead of the SkysongItems API, so it can be tested without SkysongItems.
 */
public interface ItemTagGateway {
    /** Whether SkysongItems manages this item; only those can carry wardrobe tags. */
    boolean isSkysongItem(ItemStack item);

    /** The item's layer value of this kind, as stored ({@code uuid:id}). */
    Optional<String> layer(ItemStack item, LayerKind kind);

    /** The item's {@code wardrobe_type}, lowercased. */
    Optional<String> type(ItemStack item);

    /** The item's {@code dye_color}, or white (no tint) when it has none. */
    Color dyeColor(ItemStack item);

    /** The item's {@code mannequin_skin}, lowercased; present only on mannequin spawners. */
    Optional<String> mannequinSkin(ItemStack item);

    /** The item's {@code mannequin_model}, as stored. */
    Optional<String> mannequinModel(ItemStack item);

    /** Writes a layer value onto a SkysongItems item, firing SkysongItems' change event for {@code player}. */
    void setLayer(Player player, ItemStack item, LayerKind kind, String value);

    /** Removes a layer from the item; false if it had none. */
    boolean removeLayer(Player player, ItemStack item, LayerKind kind);
}
