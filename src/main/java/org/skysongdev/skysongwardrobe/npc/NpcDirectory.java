package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Finds Citizens NPCs the ways a player can point at one. Main thread only. */
public interface NpcDirectory {
    /** The NPC with this Citizens number, as in {@code /npc sel 12}. */
    Optional<NpcInfo> byNumber(int number);

    /** The NPC with this Citizens UUID. */
    Optional<NpcInfo> byId(UUID id);

    /** The NPC the sender selected with {@code /npc sel}. */
    Optional<NpcInfo> selected(CommandSender sender);

    /** The NPC the player is looking at, within reach. */
    Optional<NpcInfo> lookedAt(Player player);
}
