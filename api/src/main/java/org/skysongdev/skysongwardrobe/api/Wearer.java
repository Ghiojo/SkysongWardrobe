package org.skysongdev.skysongwardrobe.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Whose wardrobe: a player, a Citizens NPC or a mannequin, and its UUID.
 *
 * @param type what wears it
 * @param id   the player's UUID, the NPC's Citizens UUID or the mannequin entity's UUID
 */
public record Wearer(@NotNull WearerType type, @NotNull UUID id) {
    public Wearer {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    public static @NotNull Wearer player(@NotNull UUID player) {
        return new Wearer(WearerType.PLAYER, player);
    }

    public static @NotNull Wearer npc(@NotNull UUID npc) {
        return new Wearer(WearerType.NPC, npc);
    }

    public static @NotNull Wearer mannequin(@NotNull UUID mannequin) {
        return new Wearer(WearerType.MANNEQUIN, mannequin);
    }
}
