package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Map;
import java.util.UUID;

/**
 * The mannequin entities in the world, by id. Kept apart so rendering can be tested without
 * real mannequins. Every method is main-thread only and does nothing for an unloaded mannequin.
 */
public interface MannequinBodies {
    /** Whether the mannequin's entity is loaded right now. */
    boolean isLoaded(UUID id);

    /** Shows the signed skin on the mannequin. */
    void showSkin(UUID id, SignedTexture texture);

    /** Puts exactly these items on the mannequin; a slot left out is emptied. */
    void dress(UUID id, Map<EquipmentSlot, ItemStack> equipment);
}
