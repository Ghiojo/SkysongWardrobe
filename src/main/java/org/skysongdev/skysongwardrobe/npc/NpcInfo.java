package org.skysongdev.skysongwardrobe.npc;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The plugin's own view of a Citizens NPC, so access rules and menus never touch Citizens.
 *
 * @param id     the NPC's Citizens UUID, which keys its wardrobe
 * @param number the id staff type in Citizens commands, such as {@code /npc sel 12}
 * @param name   the NPC's name, without colours
 * @param owner  the player Citizens lists as its owner; empty when the server owns it
 */
public record NpcInfo(UUID id, int number, String name, Optional<UUID> owner) {
    public NpcInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
    }
}
