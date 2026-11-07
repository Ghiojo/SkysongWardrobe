package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Draws a player's outfit onto their base skin. The wardrobe, its items' tags and the current
 * texture are read on the main thread; the base download and the {@link OutfitRenderer} run off it. An outfit
 * signed before is applied from the cache, a new one is queued for MineSkin, and an empty
 * outfit puts the base skin back.
 */
public final class SkinRenderer {
    private final WardrobeRepository wardrobes;
    private final PluginConfiguration configuration;
    private final BaseSkins bases;
    private final OutfitRenderer outfits;
    private final SkinApplier applier;
    private final TaskRunner tasks;
    private final Server server;
    private final Logger logger;

    public SkinRenderer(WardrobeRepository wardrobes, PluginConfiguration configuration, BaseSkins bases,
                        OutfitRenderer outfits, SkinApplier applier, TaskRunner tasks, Server server, Logger logger) {
        this.wardrobes = wardrobes;
        this.configuration = configuration;
        this.bases = bases;
        this.outfits = outfits;
        this.applier = applier;
        this.tasks = tasks;
        this.server = server;
        this.logger = logger;
    }

    /** Renders the player's outfit. Call on the main thread. */
    public void render(Player player) {
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty()) {
            return;
        }
        UUID owner = player.getUniqueId();
        Optional<SignedTexture> current = PlayerSkins.current(player);
        List<ItemLayers> worn = outfits.read(wardrobes.load(owner).worn(layout.get()));
        tasks.async(() -> render(owner, current, worn));
    }

    private void render(UUID owner, Optional<SignedTexture> current, List<ItemLayers> worn) {
        Optional<BaseSkin> base = bases.resolve(owner, current);
        if (base.isEmpty()) {
            return;
        }
        OutfitRenderer.Outcome outcome = outfits.render(owner, WearerKind.PLAYER, base.get().image(),
                base.get().armModel(), Optional.of(base.get().texture()), worn);
        switch (outcome) {
            case OutfitRenderer.Outcome.Ready ready -> tasks.sync(() -> applier.apply(owner, ready.texture()));
            case OutfitRenderer.Outcome.Queued queued -> tasks.sync(() -> tellQueued(owner));
            case OutfitRenderer.Outcome.NoApiKey none -> {
                if (configuration.settings().verbose()) {
                    logger.info("Not rendering the outfit of " + owner + ": config.yml has no MineSkin API key.");
                }
            }
        }
    }

    private void tellQueued(UUID owner) {
        Player online = server.getPlayer(owner);
        if (online != null) {
            online.sendMessage(configuration.messages().get("render.queued"));
        }
    }
}
