package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.skysongdev.skysongwardrobe.render.RenderListener;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts signed skins on mannequins. A mannequin whose chunk unloaded before its skin was signed
 * gets it when the chunk loads again; the world then saves it with the entity.
 */
public final class MannequinSkinApplier implements RenderListener, Listener {
    private final MannequinBodies bodies;
    private final TaskRunner tasks;
    private final Map<UUID, SignedTexture> pending = new ConcurrentHashMap<>();

    public MannequinSkinApplier(MannequinBodies bodies, TaskRunner tasks) {
        this.bodies = bodies;
        this.tasks = tasks;
    }

    @Override
    public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
        tasks.sync(() -> {
            if (bodies.isLoaded(wearer)) {
                pending.remove(wearer);
                bodies.showSkin(wearer, texture);
            } else {
                pending.put(wearer, texture);
            }
        });
    }

    @Override
    public void failed(UUID wearer, WearerKind kind, String reason) {
        // The queue logged it; the mannequin keeps the skin it has.
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            SignedTexture texture = pending.remove(entity.getUniqueId());
            if (texture != null) {
                bodies.showSkin(entity.getUniqueId(), texture);
            }
        }
    }
}
