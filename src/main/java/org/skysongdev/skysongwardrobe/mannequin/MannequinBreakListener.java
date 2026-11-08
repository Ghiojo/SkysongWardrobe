package org.skysongdev.skysongwardrobe.mannequin;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.skysongdev.skysongwardrobe.config.Messages;

import java.util.function.Supplier;

/**
 * Sneaking and punching a mannequin breaks it; a plain punch only reminds the player how. No
 * other damage (fire, explosions, mobs, falling) ever reaches a mannequin.
 */
public final class MannequinBreakListener implements Listener {
    private final MannequinBreaker breaker;
    private final MannequinMarker marker;
    private final Supplier<Messages> messages;

    public MannequinBreakListener(MannequinBreaker breaker, MannequinMarker marker, Supplier<Messages> messages) {
        this.breaker = breaker;
        this.marker = marker;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        if (!marker.isMarked(event.getAttacked())) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().isSneaking()) {
            event.getPlayer().sendActionBar(messages.get().get("mannequin.break-hint"));
            return;
        }
        if (breaker.breakBy(event.getPlayer(), event.getAttacked()) == MannequinBreaker.Result.NO_ACCESS) {
            messages.get().send(event.getPlayer(), "mannequin.no-access");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (marker.isMarked(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
