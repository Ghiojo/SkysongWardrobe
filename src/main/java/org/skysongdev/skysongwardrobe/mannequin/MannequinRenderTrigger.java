package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongwardrobe.wardrobe.MannequinUpdateEvent;

/** Dresses and renders a mannequin again whenever its wardrobe changed. */
public final class MannequinRenderTrigger implements Listener {
    private final MannequinRenderer renderer;

    public MannequinRenderTrigger(MannequinRenderer renderer) {
        this.renderer = renderer;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpdate(MannequinUpdateEvent event) {
        renderer.render(event.getMannequin());
    }
}
