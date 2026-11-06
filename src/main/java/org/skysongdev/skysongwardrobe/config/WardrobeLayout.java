package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The wardrobe menu as wardrobe.yml describes it: a 54-slot menu whose positions are all
 * filler except the wardrobe slots. Built by {@link LayoutParser}, which has already checked it.
 *
 * @param title          the menu title, in MiniMessage
 * @param fillerMaterial the item in every position that isn't a slot
 * @param fillerName     the filler item's name, in MiniMessage
 * @param slots          the wardrobe slots, ordered by priority, lowest first
 */
public record WardrobeLayout(String title, Material fillerMaterial, String fillerName, List<SlotDefinition> slots) {
    /** The number of positions in the wardrobe menu. */
    public static final int SIZE = 54;

    public WardrobeLayout {
        slots = slots.stream().sorted(Comparator.comparingInt(SlotDefinition::priority)).toList();
    }

    /** The slot at this menu position, empty for a filler position. */
    public Optional<SlotDefinition> slotAt(int position) {
        return slots.stream().filter(slot -> slot.position() == position).findFirst();
    }
}
