package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lets the mannequin a player is placing spawn even where mob spawning is turned off, such as
 * a WorldGuard region with {@code mob-spawning deny} or WorldGuard's {@code block-plugin-spawning}.
 * Those rules stop mobs; a mannequin is furniture the player already had permission to place.
 * Only the one entity being placed right now is let through, so every other spawn is untouched.
 */
public final class MannequinSpawnGuard implements Listener {
    private UUID placing;

    /** Runs {@code spawn}, letting through the entity it marks with {@link #expect}, then forgets it. */
    public <T> T allowWhile(Supplier<T> spawn) {
        try {
            return spawn.get();
        } finally {
            placing = null;
        }
    }

    /** Marks this entity as the one being placed; call it before the entity is added to the world. */
    public void expect(UUID entity) {
        placing = entity;
    }

    /** The entity being placed, if any. */
    Optional<UUID> placing() {
        return Optional.ofNullable(placing);
    }

    // HIGHEST runs after WorldGuard, which cancels at HIGH.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled() && event.getEntity().getUniqueId().equals(placing)) {
            event.setCancelled(false);
        }
    }
}
