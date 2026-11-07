package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mannequin bodies that remember what they were shown and given to wear. */
final class FakeMannequinBodies implements MannequinBodies {
    final Set<UUID> loaded = new HashSet<>();
    final Map<UUID, SignedTexture> skins = new HashMap<>();
    final Map<UUID, Map<EquipmentSlot, ItemStack>> equipment = new HashMap<>();

    @Override
    public boolean isLoaded(UUID id) {
        return loaded.contains(id);
    }

    @Override
    public void showSkin(UUID id, SignedTexture texture) {
        if (loaded.contains(id)) {
            skins.put(id, texture);
        }
    }

    @Override
    public void dress(UUID id, Map<EquipmentSlot, ItemStack> worn) {
        if (loaded.contains(id)) {
            equipment.put(id, Map.copyOf(worn));
        }
    }
}
