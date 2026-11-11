package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;

/**
 * Right-clicking a mannequin opens its wardrobe through {@link MannequinOpener}, for whoever
 * {@link MannequinAccess} allows. Only one person may have a mannequin's wardrobe open at a time,
 * so two can't take the same item.
 */
public final class MannequinInteractListener implements Listener {
    private final MannequinMarker marker;
    private final MannequinOpener opener;

    public MannequinInteractListener(MannequinMarker marker, MannequinOpener opener) {
        this.marker = marker;
        this.opener = opener;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!marker.isMarked(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        OpenOutcome outcome = opener.open(event.getPlayer(), event.getRightClicked(), true);
        // A marked entity whose mannequin is gone is cleaned up elsewhere; nothing to say.
        if (outcome != OpenOutcome.NOT_FOUND) {
            opener.tell(event.getPlayer(), outcome);
        }
    }
}
