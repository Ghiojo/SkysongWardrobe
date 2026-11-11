package org.skysongdev.skysongwardrobe.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * One more reason a player may open an NPC's wardrobe, such as working for the business the NPC
 * belongs to. A player gets in when any rule allows them: staff, the NPC's Citizens owner, or
 * any registered rule. Rules can only let more players in, never keep anyone out; cancel
 * {@code WardrobeOpenEvent} for that. Called on the main thread, so keep it quick.
 */
@FunctionalInterface
public interface NpcAccessRule {
    boolean allows(@NotNull Player viewer, @NotNull NpcView npc);
}
