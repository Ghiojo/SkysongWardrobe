package org.skysongdev.skysongwardrobe.api;

/** What kind of thing wears a wardrobe. Each keeps its wardrobes apart. */
public enum WearerType {
    /** A player, by their UUID. */
    PLAYER,
    /** A Citizens player NPC, by its Citizens UUID. */
    NPC,
    /** A placed mannequin, by its entity UUID. */
    MANNEQUIN
}
