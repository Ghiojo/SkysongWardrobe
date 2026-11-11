package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.Server;
import org.skysongdev.skysongwardrobe.api.Wearer;
import org.skysongdev.skysongwardrobe.api.WearerType;
import org.skysongdev.skysongwardrobe.api.event.OutfitRenderedEvent;
import org.skysongdev.skysongwardrobe.render.RenderListener;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.util.UUID;

/**
 * Tells other plugins each finished render with {@link OutfitRenderedEvent}. It hears renders
 * on any thread, as soon as they are signed, while the appliers put the skin on in the next
 * tick; so the event waits one tick more and always comes after the skin is on.
 */
public final class RenderEvents implements RenderListener {
    private final Server server;
    private final TaskRunner tasks;

    public RenderEvents(Server server, TaskRunner tasks) {
        this.server = server;
        this.tasks = tasks;
    }

    @Override
    public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
        fireAfterApplying(new OutfitRenderedEvent(wearer(wearer, kind), null));
    }

    @Override
    public void failed(UUID wearer, WearerKind kind, String reason) {
        fireAfterApplying(new OutfitRenderedEvent(wearer(wearer, kind), reason == null ? "unknown" : reason));
    }

    private void fireAfterApplying(OutfitRenderedEvent event) {
        tasks.sync(() -> tasks.sync(() -> server.getPluginManager().callEvent(event)));
    }

    private static Wearer wearer(UUID id, WearerKind kind) {
        return new Wearer(switch (kind) {
            case PLAYER -> WearerType.PLAYER;
            case NPC -> WearerType.NPC;
            case MANNEQUIN -> WearerType.MANNEQUIN;
        }, id);
    }
}
