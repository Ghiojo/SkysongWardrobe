package org.skysongdev.skysongwardrobe.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongwardrobe.api.Wearer;

/**
 * A wardrobe is about to open for a player, after every SkysongWardrobe check passed. Cancel
 * it to keep the wardrobe closed: the opener then gets {@code OpenResult.CANCELLED} and
 * SkysongWardrobe says nothing, so the cancelling plugin explains why. Fired on the main thread.
 */
public final class WardrobeOpenEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player viewer;
    private final Wearer wearer;
    private final OpenSource source;
    private boolean cancelled;

    public WardrobeOpenEvent(@NotNull Player viewer, @NotNull Wearer wearer, @NotNull OpenSource source) {
        this.viewer = viewer;
        this.wearer = wearer;
        this.source = source;
    }

    /** Who will see the wardrobe. */
    public @NotNull Player getViewer() {
        return viewer;
    }

    /** Whose wardrobe it is. */
    public @NotNull Wearer getWearer() {
        return wearer;
    }

    /** What asked to open it. */
    public @NotNull OpenSource getSource() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
