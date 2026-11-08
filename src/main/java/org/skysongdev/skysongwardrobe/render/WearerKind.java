package org.skysongdev.skysongwardrobe.render;

/**
 * Who a render is for, in the order the render queue serves them: players first, then Citizens
 * NPCs, then mannequins. Only the name is saved in the queue file, so the order can change.
 */
public enum WearerKind {
    PLAYER,
    NPC,
    MANNEQUIN
}
