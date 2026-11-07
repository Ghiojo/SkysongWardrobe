package org.skysongdev.skysongwardrobe.render;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeUpdateEvent;

/**
 * Renders a player's skin whenever their outfit may have changed: on closing a changed
 * wardrobe, on a staff edit, and on joining. Forgets their base skin when they leave.
 */
public final class RenderTrigger implements Listener {
    private final SkinRenderer renderer;
    private final BaseSkins bases;

    public RenderTrigger(SkinRenderer renderer, BaseSkins bases) {
        this.renderer = renderer;
        this.bases = bases;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpdate(WardrobeUpdateEvent event) {
        renderer.render(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        bases.forget(event.getPlayer().getUniqueId());
    }
}
