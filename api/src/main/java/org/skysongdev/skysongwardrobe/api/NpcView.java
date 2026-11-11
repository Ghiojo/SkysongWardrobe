package org.skysongdev.skysongwardrobe.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A Citizens NPC as SkysongWardrobe sees it, so plugins using the API never need Citizens'
 * classes for it.
 *
 * @param id     the NPC's Citizens UUID, which keys its wardrobe
 * @param number the id staff type in Citizens commands, such as {@code /npc sel 12}
 * @param name   the NPC's name, without colours
 * @param owner  the player Citizens lists as its owner; empty when the server owns it
 */
public record NpcView(@NotNull UUID id, int number, @NotNull String name, @NotNull Optional<UUID> owner) {
    public NpcView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
    }
}
