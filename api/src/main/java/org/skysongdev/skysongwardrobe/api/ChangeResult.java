package org.skysongdev.skysongwardrobe.api;

/** How a change to an outfit went: {@link #CHANGED}, or why nothing changed. */
public enum ChangeResult {
    /** Saved; the wearer's skin renders again, as when a player closes the menu. */
    CHANGED,
    /** No such NPC in Citizens, or no such placed mannequin. */
    NOT_FOUND,
    /** wardrobe.yml has no slot with that id. */
    UNKNOWN_SLOT,
    /** The item has no wardrobe type (the wardrobe_type tag), so it can't be worn. */
    NOT_WEARABLE,
    /** The slot doesn't accept the item's wardrobe type. */
    WRONG_SLOT,
    /** wardrobe.yml has errors, so every wardrobe is closed until staff fix it. */
    WARDROBE_CLOSED,
    /** NPC wardrobes need Citizens, which is not installed or failed to hook. */
    UNAVAILABLE
}
