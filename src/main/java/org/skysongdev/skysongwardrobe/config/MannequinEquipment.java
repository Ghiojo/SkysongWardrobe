package org.skysongdev.skysongwardrobe.config;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The locked equipment slots of a mannequin's wardrobe menu. They always sit in the last two
 * columns, which wardrobe.yml can't use: armour down the eighth column, hands in the ninth
 * beside the chest and legs. Their items are stored in the mannequin's wardrobe under
 * {@link #wardrobeId()}, which no wardrobe.yml slot can be named, so they are never drawn as layers.
 */
public enum MannequinEquipment {
    HEAD(7, EquipmentSlot.HEAD),
    CHEST(16, EquipmentSlot.CHEST),
    LEGS(25, EquipmentSlot.LEGS),
    FEET(34, EquipmentSlot.FEET),
    MAIN_HAND(17, EquipmentSlot.HAND),
    OFF_HAND(26, EquipmentSlot.OFF_HAND);

    /** Menu columns from this one (0-based) to the right are kept for equipment. */
    public static final int FIRST_RESERVED_COLUMN = 7;
    private static final int COLUMNS = 9;
    /** Starts every equipment key in a wardrobe; wardrobe.yml slot ids may not. */
    public static final String ID_PREFIX = "equipment:";

    private final int position;
    private final EquipmentSlot slot;

    MannequinEquipment(int position, EquipmentSlot slot) {
        this.position = position;
        this.slot = slot;
    }

    public int position() {
        return position;
    }

    /** Where the mannequin entity wears the item. */
    public EquipmentSlot slot() {
        return slot;
    }

    /** The key the item is stored under in the mannequin's wardrobe. */
    public String wardrobeId() {
        return ID_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    /** Whether the item can be worn here: anything in a hand, only matching armour elsewhere. */
    public boolean fits(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (slot.isHand()) {
            return true;
        }
        EquipmentSlot natural = item.hasItemMeta() && item.getItemMeta().hasEquippable()
                ? item.getItemMeta().getEquippable().getSlot()
                : item.getType().getEquipmentSlot();
        return natural == slot;
    }

    /** The equipment slot at a menu position, empty for any other position. */
    public static Optional<MannequinEquipment> at(int position) {
        return Arrays.stream(values()).filter(equipment -> equipment.position == position).findFirst();
    }

    /** Whether a menu position is in the columns kept for equipment. */
    public static boolean isReserved(int position) {
        return position % COLUMNS >= FIRST_RESERVED_COLUMN;
    }
}
