package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.util.Optional;
import java.util.UUID;

/**
 * Puts finished skins on players, on the main thread. A player who left before their skin was
 * signed gets it on their next join: the join re-renders the outfit and finds it in the cache.
 */
public final class SkinApplier implements RenderListener {
    private final Server server;
    private final TaskRunner tasks;

    public SkinApplier(Server server, TaskRunner tasks) {
        this.server = server;
        this.tasks = tasks;
    }

    @Override
    public void signed(UUID player, WearerKind kind, SignedTexture texture) {
        tasks.sync(() -> apply(player, texture));
    }

    @Override
    public void failed(UUID player, WearerKind kind, String reason) {
        // The queue logged it; the player keeps the skin they have.
    }

    /** Puts the texture on the player now, if online and not already wearing it. Main thread only. */
    public void apply(UUID player, SignedTexture texture) {
        Player online = server.getPlayer(player);
        if (online != null && !PlayerSkins.current(online).equals(Optional.of(texture))) {
            PlayerSkins.apply(online, texture);
        }
    }
}
