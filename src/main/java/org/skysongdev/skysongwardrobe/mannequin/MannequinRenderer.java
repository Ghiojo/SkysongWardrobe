package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.MannequinEquipment;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Dresses a mannequin and renders its skin: equipment goes on at once, and the worn layers are
 * drawn over its preset base skin through the shared {@link OutfitRenderer}. Even a mannequin
 * wearing nothing is rendered, since its plain base skin needs signing too.
 */
public final class MannequinRenderer {
    private final MannequinRepository mannequins;
    private final WardrobeRepository wardrobes;
    private final PluginConfiguration configuration;
    private final OutfitRenderer outfits;
    private final MannequinBodies bodies;
    private final TaskRunner tasks;
    private final Logger logger;

    public MannequinRenderer(MannequinRepository mannequins, WardrobeRepository wardrobes,
                             PluginConfiguration configuration, OutfitRenderer outfits, MannequinBodies bodies,
                             TaskRunner tasks, Logger logger) {
        this.mannequins = mannequins;
        this.wardrobes = wardrobes;
        this.configuration = configuration;
        this.outfits = outfits;
        this.bodies = bodies;
        this.tasks = tasks;
        this.logger = logger;
    }

    /** Dresses and renders the mannequin. Main thread only. */
    public void render(UUID id) {
        Optional<Mannequin> mannequin = mannequins.find(id);
        Optional<WardrobeLayout> layout = configuration.layout();
        if (mannequin.isEmpty() || layout.isEmpty()) {
            return;
        }
        Wardrobe wardrobe = wardrobes.load(id);
        bodies.dress(id, equipment(wardrobe));
        Optional<BufferedImage> base = configuration.mannequinSkins().image(mannequin.get().skin());
        if (base.isEmpty()) {
            logger.warning("Mannequin " + id + " wears the skin " + mannequin.get().skin()
                    + ", which is not in mannequin-skins/, so its outfit is not drawn.");
            return;
        }
        List<ItemStack> worn = wardrobe.worn(layout.get());
        tasks.async(() -> {
            OutfitRenderer.Outcome outcome = outfits.render(id, WearerKind.MANNEQUIN, base.get(), mannequin.get().model(),
                    Optional.empty(), worn);
            if (outcome instanceof OutfitRenderer.Outcome.Ready ready) {
                tasks.sync(() -> bodies.showSkin(id, ready.texture()));
            } else if (outcome instanceof OutfitRenderer.Outcome.NoApiKey && configuration.settings().verbose()) {
                logger.info("Not rendering mannequin " + id + ": config.yml has no MineSkin API key.");
            }
        });
    }

    private static Map<EquipmentSlot, ItemStack> equipment(Wardrobe wardrobe) {
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        for (MannequinEquipment slot : MannequinEquipment.values()) {
            wardrobe.item(slot.wardrobeId()).ifPresent(item -> equipment.put(slot.slot(), item));
        }
        return equipment;
    }
}
