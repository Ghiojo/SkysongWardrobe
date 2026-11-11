package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Renders an NPC again whenever its wardrobe changed. */
public final class NpcRenderTrigger implements Listener {
    private final NpcRenderer renderer;

    public NpcRenderTrigger(NpcRenderer renderer) {
        this.renderer = renderer;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpdate(NpcUpdateEvent event) {
        renderer.render(event.getNpc(), event.getEditor() == null ? null : event.getEditor().getUniqueId());
    }
}
