package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/** A mannequin's wardrobe changed while its menu was open, so it needs dressing and rendering again. */
public final class MannequinUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID mannequin;

    public MannequinUpdateEvent(UUID mannequin) {
        this.mannequin = mannequin;
    }

    /** The mannequin's id. */
    public UUID getMannequin() {
        return mannequin;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
