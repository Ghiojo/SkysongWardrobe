package org.skysongdev.skysongwardrobe.mannequin;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Server;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** {@link MannequinBodies} backed by Paper's mannequin entities. */
public final class PaperMannequinBodies implements MannequinBodies {
    private static final String TEXTURES = "textures";

    private final Server server;

    public PaperMannequinBodies(Server server) {
        this.server = server;
    }

    @Override
    public boolean isLoaded(UUID id) {
        return entity(id).isPresent();
    }

    @Override
    public void showSkin(UUID id, SignedTexture texture) {
        entity(id).ifPresent(mannequin -> mannequin.setProfile(ResolvableProfile.resolvableProfile()
                .addProperty(new ProfileProperty(TEXTURES, texture.value(), texture.signature()))
                .build()));
    }

    @Override
    public void dress(UUID id, Map<EquipmentSlot, ItemStack> equipment) {
        entity(id).ifPresent(mannequin -> {
            EntityEquipment worn = mannequin.getEquipment();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (mannequin.canUseEquipmentSlot(slot)) {
                    // No drop chance: a mannequin isn't a mob, so Paper refuses one. It never dies
                    // anyway, and breaking it drops its wardrobe instead.
                    worn.setItem(slot, equipment.getOrDefault(slot, ItemStack.empty()));
                }
            }
        });
    }

    private Optional<Mannequin> entity(UUID id) {
        return server.getEntity(id) instanceof Mannequin mannequin ? Optional.of(mannequin) : Optional.empty();
    }
}
