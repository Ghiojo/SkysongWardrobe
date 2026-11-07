package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A player's outfit may look different now: their wardrobe changed while a menu was open, or
 * they joined and Minecraft gave them their own skin back. The renderer listens for this.
 */
public final class WardrobeUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public WardrobeUpdateEvent(Player player) {
        this.player = player;
    }

    /** The wardrobe's owner, who is online. */
    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
