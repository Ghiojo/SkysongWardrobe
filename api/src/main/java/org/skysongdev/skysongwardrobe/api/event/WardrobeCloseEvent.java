package org.skysongdev.skysongwardrobe.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongwardrobe.api.Wearer;

/**
 * A player closed a wardrobe menu, however it closed (including logging out). When it
 * {@link #hasChanged() changed}, the wearer's skin is rendering again. Fired on the main thread,
 * while the menu is still closing: open other menus a tick later, or use
 * {@code OpenOptions.onClose}, which already waits that tick.
 */
public final class WardrobeCloseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player viewer;
    private final Wearer wearer;
    private final boolean changed;

    public WardrobeCloseEvent(@NotNull Player viewer, @NotNull Wearer wearer, boolean changed) {
        this.viewer = viewer;
        this.wearer = wearer;
        this.changed = changed;
    }

    /** Who closed it. */
    public @NotNull Player getViewer() {
        return viewer;
    }

    /** Whose wardrobe it was. */
    public @NotNull Wearer getWearer() {
        return wearer;
    }

    /** Whether any item went in or out while it was open. */
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
