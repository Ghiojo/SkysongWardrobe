package org.skysongdev.skysongwardrobe.config;

import java.util.Locale;
import java.util.Set;

/**
 * One wardrobe slot from wardrobe.yml.
 *
 * @param id       the slot's key in wardrobe.yml, such as {@code coat}
 * @param position the inventory position of the slot, 0 to 53
 * @param accepts  the {@code wardrobe_type} values an item needs to go in, lowercased
 * @param priority the draw order of the slot's layers; higher is drawn over lower
 */
public record SlotDefinition(String id, int position, Set<String> accepts, int priority) {

    public SlotDefinition {
        accepts = Set.copyOf(accepts);
    }

    /** Whether an item with this {@code wardrobe_type} may go in this slot, ignoring case. */
    public boolean accepts(String wardrobeType) {
        return wardrobeType != null && accepts.contains(wardrobeType.toLowerCase(Locale.ROOT));
    }
}
