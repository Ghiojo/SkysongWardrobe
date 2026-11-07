package org.skysongdev.skysongwardrobe.items;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An {@link ItemTagGateway} where an item's wardrobe_type is decided by its material. */
public final class FakeItemTags implements ItemTagGateway {
    private final Map<Material, String> types = new HashMap<>();

    public FakeItemTags type(Material material, String type) {
        types.put(material, type);
        return this;
    }

    @Override
    public boolean isSkysongItem(ItemStack item) {
        return item != null && types.containsKey(item.getType());
    }

    @Override
    public Optional<String> layer(ItemStack item, LayerKind kind) {
        return Optional.empty();
    }

    @Override
    public Optional<String> type(ItemStack item) {
        return Optional.ofNullable(types.get(item.getType()));
    }

    @Override
    public Color dyeColor(ItemStack item) {
        return Color.WHITE;
    }

    @Override
    public void setLayer(Player player, ItemStack item, LayerKind kind, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeLayer(Player player, ItemStack item, LayerKind kind) {
        throw new UnsupportedOperationException();
    }
}
