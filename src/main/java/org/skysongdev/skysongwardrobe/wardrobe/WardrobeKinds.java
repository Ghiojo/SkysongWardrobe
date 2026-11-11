package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Where each kind of wardrobe is saved, and what happens once one changed. The menu listener
 * asks here instead of knowing every kind, so a new kind of wearer only registers itself.
 */
public final class WardrobeKinds {
    /**
     * One kind's storage, and what to do once a wearer's wardrobe changed: {@code changed} gets
     * the wearer's id and the player who closed the menu, or null when another plugin changed it.
     */
    public record Kind(WardrobeRepository wardrobes, BiConsumer<UUID, Player> changed) {
    }

    private final Map<WardrobeKind, Kind> kinds = new EnumMap<>(WardrobeKind.class);

    /**
     * Players and mannequins: a changed player wardrobe fires {@link WardrobeUpdateEvent} while
     * its owner is online, and a changed mannequin wardrobe fires {@link MannequinUpdateEvent}.
     */
    public static WardrobeKinds of(WardrobeRepository players, WardrobeRepository mannequins, Server server) {
        return new WardrobeKinds()
                .register(WardrobeKind.PLAYER, players, (owner, editor) -> {
                    Player online = server.getPlayer(owner);
                    if (online != null) {
                        server.getPluginManager().callEvent(new WardrobeUpdateEvent(online));
                    }
                })
                .register(WardrobeKind.MANNEQUIN, mannequins,
                        (mannequin, editor) -> server.getPluginManager().callEvent(new MannequinUpdateEvent(mannequin)));
    }

    /** Adds or replaces a kind. */
    public WardrobeKinds register(WardrobeKind kind, WardrobeRepository wardrobes, BiConsumer<UUID, Player> changed) {
        kinds.put(kind, new Kind(wardrobes, changed));
        return this;
    }

    /** The kind's storage and change handler; empty if nothing registered it. */
    public Optional<Kind> get(WardrobeKind kind) {
        return Optional.ofNullable(kinds.get(kind));
    }
}
