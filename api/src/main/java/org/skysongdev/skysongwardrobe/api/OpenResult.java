package org.skysongdev.skysongwardrobe.api;

/** How a request to open a wardrobe went: {@link #OPENED}, or why it did not open. */
public enum OpenResult {
    /** The viewer now sees the wardrobe. */
    OPENED,
    /**
     * No such wearer: no Citizens NPC with that number or id, or no placed mannequin with that
     * entity id in a loaded chunk.
     */
    NOT_FOUND,
    /** The Citizens NPC is not a player NPC, so it can't wear a wardrobe. */
    NOT_A_PLAYER_NPC,
    /** The access rules don't let the viewer open this wardrobe. Never returned with {@link Access#BYPASS}. */
    NO_ACCESS,
    /** Someone else is dressing this NPC or mannequin right now; only one player may at a time. */
    IN_USE,
    /** wardrobe.yml has errors, so every wardrobe is closed until staff fix it. */
    WARDROBE_CLOSED,
    /** NPC wardrobes need Citizens, which is not installed or failed to hook. */
    UNAVAILABLE
}
