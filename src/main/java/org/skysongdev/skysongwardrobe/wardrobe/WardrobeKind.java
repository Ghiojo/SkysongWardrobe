package org.skysongdev.skysongwardrobe.wardrobe;

import org.skysongdev.skysongwardrobe.api.WearerType;

import java.util.Optional;

/** Whose wardrobe a menu shows. Each kind keeps its wardrobes apart, keyed by the wearer's UUID. */
public enum WardrobeKind {
    PLAYER(false, true, false),
    MANNEQUIN(true, true, true),
    NPC(false, true, true),
    /** A removed NPC's archived wardrobe, which items only come out of. */
    REMOVED_NPC(false, false, false);

    private final boolean equipment;
    private final boolean acceptsItems;
    private final boolean exclusive;

    WardrobeKind(boolean equipment, boolean acceptsItems, boolean exclusive) {
        this.equipment = equipment;
        this.acceptsItems = acceptsItems;
        this.exclusive = exclusive;
    }

    /**
     * Whether only one player may have a wearer's wardrobe open at a time, so two can't take the
     * same item. A player's own wardrobe may be open for its owner and staff together.
     */
    public boolean exclusive() {
        return exclusive;
    }

    /** Who wears this kind of wardrobe, as the API names it; empty for a removed NPC's archive. */
    public Optional<WearerType> wearerType() {
        return switch (this) {
            case PLAYER -> Optional.of(WearerType.PLAYER);
            case MANNEQUIN -> Optional.of(WearerType.MANNEQUIN);
            case NPC -> Optional.of(WearerType.NPC);
            case REMOVED_NPC -> Optional.empty();
        };
    }

    /** Whether items may go in, or only come out. */
    public boolean acceptsItems() {
        return acceptsItems;
    }

    /** Whether the menu also has the locked equipment slots for armour and held items. */
    public boolean hasEquipment() {
        return equipment;
    }
}
