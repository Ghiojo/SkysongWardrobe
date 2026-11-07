package org.skysongdev.skysongwardrobe.items;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An {@link ItemTagGateway} where an item's wardrobe_type is decided by its material, and
 * written layers are remembered per kind, as if every write went to the same item.
 */
public final class FakeItemTags implements ItemTagGateway {
    private final Map<Material, String> types = new HashMap<>();
    private final Map<LayerKind, String> layers = new HashMap<>();
    private final Map<Material, String> mannequinSkins = new HashMap<>();
    private final Map<Material, String> mannequinModels = new HashMap<>();

    /** Makes every item of the material a mannequin spawner with this skin and model. */
    public FakeItemTags spawner(Material material, String skin, String model) {
        types.put(material, "");
        mannequinSkins.put(material, skin);
        mannequinModels.put(material, model);
        return this;
    }

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
        return Optional.ofNullable(layers.get(kind));
    }

    @Override
    public Optional<String> type(ItemStack item) {
        return Optional.ofNullable(types.get(item.getType())).filter(type -> !type.isEmpty());
    }

    @Override
    public Optional<String> mannequinSkin(ItemStack item) {
        return Optional.ofNullable(mannequinSkins.get(item.getType()));
    }

    @Override
    public Optional<String> mannequinModel(ItemStack item) {
        return Optional.ofNullable(mannequinModels.get(item.getType()));
    }

    @Override
    public Color dyeColor(ItemStack item) {
        return Color.WHITE;
    }

    @Override
    public void setLayer(Player player, ItemStack item, LayerKind kind, String value) {
        layers.put(kind, value);
    }

    @Override
    public boolean removeLayer(Player player, ItemStack item, LayerKind kind) {
        return layers.remove(kind) != null;
    }
}
