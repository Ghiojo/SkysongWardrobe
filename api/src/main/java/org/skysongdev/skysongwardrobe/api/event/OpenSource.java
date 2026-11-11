package org.skysongdev.skysongwardrobe.api.event;

/** What asked to open a wardrobe. */
public enum OpenSource {
    /** A command: {@code /wardrobe} or {@code /sw npc}, typed by a player or run from the console. */
    COMMAND,
    /** A player right-clicked a mannequin. */
    INTERACT,
    /** Another plugin, through the API. */
    API
}
