package org.skysongdev.skysongwardrobe.api;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What players, NPCs and mannequins wear, by wardrobe slot id (the slot ids of wardrobe.yml).
 * Items go in and come out as copies of one item each. A change follows the same slot rules as
 * the menu, is saved, shows at once in any open menu on that wardrobe, and renders the wearer's
 * skin again. Reading a wardrobe that isn't loaded yet (an offline player's, say) reads it from
 * storage first. Main thread only.
 */
@ApiStatus.NonExtendable
public interface Outfits {

    /**
     * The wardrobe slot ids of wardrobe.yml in the order their layers are drawn, lowest priority
     * first; empty while wardrobe.yml has errors.
     */
    @NotNull List<String> slots();

    /** Every item the wearer has in the wardrobe, by slot id. */
    @NotNull Map<String, ItemStack> outfit(@NotNull Wearer wearer);

    /** The item in one slot of the wearer's wardrobe. */
    @NotNull Optional<ItemStack> item(@NotNull Wearer wearer, @NotNull String slotId);

    /**
     * Puts one of {@code item} into the slot, replacing whatever was there. The replaced item is
     * gone: read it with {@link #item} first to give it back to someone. A null or empty item
     * clears the slot, as {@link #clear} does.
     */
    @NotNull ChangeResult setItem(@NotNull Wearer wearer, @NotNull String slotId, @Nullable ItemStack item);

    /** Empties the slot. */
    @NotNull ChangeResult clear(@NotNull Wearer wearer, @NotNull String slotId);
}
