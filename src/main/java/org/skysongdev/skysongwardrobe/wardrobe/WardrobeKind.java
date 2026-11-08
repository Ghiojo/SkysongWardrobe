package org.skysongdev.skysongwardrobe.wardrobe;

/** Whose wardrobe a menu shows. Each kind keeps its wardrobes apart, keyed by the wearer's UUID. */
public enum WardrobeKind {
    PLAYER(false, true),
    MANNEQUIN(true, true),
    NPC(false, true),
    /** A removed NPC's archived wardrobe, which items only come out of. */
    REMOVED_NPC(false, false);

    private final boolean equipment;
    private final boolean acceptsItems;

    WardrobeKind(boolean equipment, boolean acceptsItems) {
        this.equipment = equipment;
        this.acceptsItems = acceptsItems;
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
