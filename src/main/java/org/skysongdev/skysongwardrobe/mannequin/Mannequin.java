package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.util.Objects;
import java.util.UUID;

/**
 * One placed mannequin. What it wears lives in its wardrobe, keyed by the same id; its position,
 * skin and equipment live on the entity, which the world saves with its chunk.
 *
 * @param id      the mannequin entity's UUID
 * @param owner   the player who placed it
 * @param spawner the exact item it was placed with, given back when it breaks
 * @param skin    the base skin id in mannequin-skins/
 * @param model   the arm model it renders with
 */
public record Mannequin(UUID id, UUID owner, ItemStack spawner, String skin, ArmModel model) {
    public Mannequin {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(skin, "skin");
        Objects.requireNonNull(model, "model");
        spawner = Objects.requireNonNull(spawner, "spawner").asOne();
    }

    /** A copy of the spawner, so the stored one can't change. */
    @Override
    public ItemStack spawner() {
        return spawner.clone();
    }
}
