package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/** An NPC's wardrobe changed while its menu was open, so its outfit needs rendering again. */
public final class NpcUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID npc;
    private final Player editor;

    public NpcUpdateEvent(UUID npc, Player editor) {
        this.npc = npc;
        this.editor = editor;
    }

    /** The NPC's Citizens UUID. */
    public UUID getNpc() {
        return npc;
    }

    /** Who closed the menu, and hears how the render went. */
    public Player getEditor() {
        return editor;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
