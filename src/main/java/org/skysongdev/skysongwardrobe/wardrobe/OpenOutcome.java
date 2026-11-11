package org.skysongdev.skysongwardrobe.wardrobe;

/** How asking to open a wardrobe went: opened, or why not. */
public enum OpenOutcome {
    OPENED,
    /** No such wearer: no NPC with that number, or no mannequin with that id. */
    NOT_FOUND,
    /** The NPC is not a player NPC, so it can't wear a wardrobe. */
    NOT_A_PLAYER_NPC,
    /** The viewer may not open this wearer's wardrobe. */
    NO_ACCESS,
    /** Someone else is dressing this wearer right now. */
    IN_USE,
    /** wardrobe.yml has errors, so every wardrobe is closed. */
    WARDROBE_CLOSED,
    /** NPC wardrobes need Citizens, which is not installed or failed to hook. */
    UNAVAILABLE,
    /** Another plugin cancelled the open event; it explains why, so nothing is said. */
    CANCELLED
}
