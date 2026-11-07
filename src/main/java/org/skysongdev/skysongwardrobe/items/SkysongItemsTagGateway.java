package org.skysongdev.skysongwardrobe.items;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongitems.api.SkysongItemsApi;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/** {@link ItemTagGateway} backed by the SkysongItems API. */
public final class SkysongItemsTagGateway implements ItemTagGateway {
    private static final int RGB_MASK = 0xFFFFFF;

    private final SkysongItemsApi api;

    public SkysongItemsTagGateway(SkysongItemsApi api) {
        this.api = api;
    }

    @Override
    public boolean isSkysongItem(ItemStack item) {
        return item != null && api.templates().isSkysongItem(item);
    }

    @Override
    public Optional<String> layer(ItemStack item, LayerKind kind) {
        return nonBlank(api.tags().getString(item, kind.tagId()));
    }

    @Override
    public Optional<String> type(ItemStack item) {
        return nonBlank(api.tags().getString(item, WardrobeTags.TYPE)).map(type -> type.toLowerCase(Locale.ROOT));
    }

    @Override
    public Color dyeColor(ItemStack item) {
        OptionalInt rgb = api.tags().getInt(item, WardrobeTags.DYE_COLOR);
        // Bits above the 24 RGB bits mean nothing to a tint, and Color.fromRGB refuses them.
        return rgb.isPresent() ? Color.fromRGB(rgb.getAsInt() & RGB_MASK) : Color.WHITE;
    }

    @Override
    public void setLayer(Player player, ItemStack item, LayerKind kind, String value) {
        api.tags().set(player, item, kind.tagId(), value);
    }

    @Override
    public boolean removeLayer(Player player, ItemStack item, LayerKind kind) {
        return api.tags().remove(player, item, kind.tagId());
    }

    private static Optional<String> nonBlank(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isEmpty());
    }
}
