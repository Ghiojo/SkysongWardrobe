package org.skysongdev.skysongwardrobe.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Citizens NPC wardrobes: finding NPCs, asking who may open them, and adding access rules.
 * Only player NPCs wear a wardrobe. Without Citizens, {@link #available()} is false, nothing is
 * found and nobody may open anything. Main thread only.
 */
@ApiStatus.NonExtendable
public interface Npcs {

    /** Whether Citizens is installed and SkysongWardrobe hooked into it. */
    boolean available();

    /** The NPC with this Citizens number, as in {@code /npc sel 12}. */
    @NotNull Optional<NpcView> find(int number);

    /** The NPC with this Citizens UUID. */
    @NotNull Optional<NpcView> find(@NotNull UUID id);

    /**
     * Whether {@code viewer} may open this NPC's wardrobe under the access rules: it is a player
     * NPC and staff, its owner or a registered rule lets them in. Use it to show or hide a button;
     * opening can still fail, for example while someone else is dressing the NPC.
     */
    boolean canOpenWardrobe(@NotNull Player viewer, int npcNumber);

    /** {@link #canOpenWardrobe(Player, int)} for the NPC with this Citizens UUID. */
    boolean canOpenWardrobe(@NotNull Player viewer, @NotNull UUID npcId);

    /**
     * Adds an access rule owned by {@code plugin}. It is removed when that plugin disables, or
     * with {@link #unregisterAccessRules(Plugin)}.
     */
    void registerAccessRule(@NotNull Plugin plugin, @NotNull NpcAccessRule rule);

    /** Removes every access rule {@code plugin} registered. */
    void unregisterAccessRules(@NotNull Plugin plugin);
}
