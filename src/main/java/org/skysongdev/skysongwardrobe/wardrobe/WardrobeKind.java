package org.skysongdev.skysongwardrobe.wardrobe;

/** Whose wardrobe a menu shows. Each kind keeps its wardrobes apart, keyed by the wearer's UUID. */
public enum WardrobeKind {
    PLAYER(false),
    MANNEQUIN(true),
    NPC(false);

    private final boolean equipment;

    WardrobeKind(boolean equipment) {
        this.equipment = equipment;
    }

    /** Whether the menu also has the locked equipment slots for armour and held items. */
    public boolean hasEquipment() {
        return equipment;
    }
}
