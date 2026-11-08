package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.event.NPCCloneEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongwardrobe.npc.NpcLifecycle;

/**
 * Passes Citizens' NPC removals and copies on to {@link NpcLifecycle}. Citizens only fires the
 * remove event when an NPC is deleted, never on a restart or a chunk unload.
 */
final class CitizensNpcListener implements Listener {
    private final NpcLifecycle lifecycle;

    CitizensNpcListener(NpcLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(NPCRemoveEvent event) {
        CitizensNpcDirectory.info(event.getNPC()).ifPresent(lifecycle::removed);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCopy(NPCCloneEvent event) {
        lifecycle.copied(event.getNPC().getUniqueId(), event.getClone().getUniqueId());
    }
}
