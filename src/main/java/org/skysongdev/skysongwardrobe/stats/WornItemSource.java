package org.skysongdev.skysongwardrobe.stats;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.api.ItemSource;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The items a player wears in their wardrobe, so SkysongStats counts their item tags like
 * items in the inventory. Only the slots wardrobe.yml lists count; while that file has an
 * error the wardrobe is closed and nothing in it counts.
 */
public final class WornItemSource implements ItemSource {
    private final WardrobeRepository wardrobes;
    private final Supplier<Optional<WardrobeLayout>> layout;

    public WornItemSource(WardrobeRepository wardrobes, Supplier<Optional<WardrobeLayout>> layout) {
        this.wardrobes = wardrobes;
        this.layout = layout;
    }

    @Override
    public Collection<ItemStack> items(Player player) {
        return layout.get()
                .map(current -> wardrobes.load(player.getUniqueId()).worn(current))
                .orElse(List.of());
    }
}
