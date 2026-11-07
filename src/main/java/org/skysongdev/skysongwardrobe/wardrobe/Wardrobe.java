package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One player's stored items, one per wardrobe slot, keyed by the slot's id in wardrobe.yml.
 * Items are copied in and out, so nothing outside can change a stored item by accident.
 */
public final class Wardrobe {
    private final UUID owner;
    private final Map<String, ItemStack> items = new HashMap<>();

    public Wardrobe(UUID owner) {
        this.owner = owner;
    }

    public UUID owner() {
        return owner;
    }

    public synchronized Optional<ItemStack> item(String slotId) {
        return Optional.ofNullable(items.get(slotId)).map(ItemStack::clone);
    }

    /** Stores one item in the slot; an empty or null item clears it. */
    public synchronized void put(String slotId, ItemStack item) {
        if (item == null || item.isEmpty()) {
            items.remove(slotId);
        } else {
            items.put(slotId, item.asOne());
        }
    }

    /** Every stored item by slot id, including slots wardrobe.yml no longer lists. */
    public synchronized Map<String, ItemStack> items() {
        Map<String, ItemStack> copy = new HashMap<>();
        items.forEach((slot, item) -> copy.put(slot, item.clone()));
        return copy;
    }

    /** The items in the layout's slots, lowest priority first: the order their layers are drawn in. */
    public synchronized List<ItemStack> worn(WardrobeLayout layout) {
        return layout.slots().stream()
                .map(SlotDefinition::id)
                .filter(items::containsKey)
                .map(slot -> items.get(slot).clone())
                .toList();
    }
}
