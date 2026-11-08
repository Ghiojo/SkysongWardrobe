package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.entity.Player;

/**
 * One reason a player may open an NPC's wardrobe. {@link NpcAccess} lets a player in when any
 * rule allows it, so a new reason, such as belonging to the business an NPC works for, is one
 * more rule and changes nothing else.
 */
@FunctionalInterface
public interface NpcAccessRule {
    boolean allows(Player viewer, NpcInfo npc);
}
