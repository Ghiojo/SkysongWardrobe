package org.skysongdev.skysongwardrobe.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongwardrobe.api.Wearer;

/**
 * A wearer's outfit finished rendering: its new skin was put on (an online player, a Citizens
 * NPC, or a mannequin, which shows it once its chunk is loaded), or MineSkin could not sign it
 * and the wearer keeps the skin it had. An outfit seen before is already signed and finishes
 * within a tick or two; a new one waits in the MineSkin queue first. Fired on the main thread,
 * the tick after the skin is put on.
 */
public final class OutfitRenderedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Wearer wearer;
    private final String failure;

    /** @param failure why signing failed; null when the skin was put on */
    public OutfitRenderedEvent(@NotNull Wearer wearer, @Nullable String failure) {
        this.wearer = wearer;
        this.failure = failure;
    }

    /** Whose outfit it is. */
    public @NotNull Wearer getWearer() {
        return wearer;
    }

    /** Whether the new skin was put on. */
    public boolean isSuccess() {
        return failure == null;
    }

    /** Why MineSkin could not sign the skin, as logged; null on success. */
    public @Nullable String getFailure() {
        return failure;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
