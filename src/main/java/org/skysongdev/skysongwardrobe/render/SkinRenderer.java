package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Draws a player's outfit onto their base skin. The wardrobe and the current texture are read
 * on the main thread; the base download, layer files and composing run off it. An outfit signed
 * before is applied from the cache, a new one is queued for MineSkin, and an empty outfit puts
 * the base skin back.
 */
public final class SkinRenderer {
    private final WardrobeRepository wardrobes;
    private final PluginConfiguration configuration;
    private final BaseSkins bases;
    private final Supplier<LayerResolver> layers;
    private final TextureCache cache;
    private final RenderQueue queue;
    private final SkinApplier applier;
    private final TaskRunner tasks;
    private final Server server;
    private final Logger logger;

    public SkinRenderer(WardrobeRepository wardrobes, PluginConfiguration configuration, BaseSkins bases,
                        Supplier<LayerResolver> layers, TextureCache cache, RenderQueue queue, SkinApplier applier,
                        TaskRunner tasks, Server server, Logger logger) {
        this.wardrobes = wardrobes;
        this.configuration = configuration;
        this.bases = bases;
        this.layers = layers;
        this.cache = cache;
        this.queue = queue;
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
        List<ItemStack> worn = wardrobes.load(owner).worn(layout.get());
        tasks.async(() -> render(owner, current, worn));
    }

    private void render(UUID owner, Optional<SignedTexture> current, List<ItemStack> worn) {
        Optional<BaseSkin> base = bases.resolve(owner, current);
        if (base.isEmpty()) {
            return;
        }
        List<Layer> drawn = layers.get().layers(worn);
        if (drawn.isEmpty()) {
            applyLater(owner, base.get().texture());
            return;
        }
        BufferedImage skin = SkinComposer.compose(base.get().image(), drawn);
        RenderJob job = RenderJob.of(owner, skin, base.get().armModel());
        Optional<SignedTexture> known = cache.find(job.hash());
        if (known.isPresent()) {
            applyLater(owner, known.get());
        } else if (configuration.settings().hasMineSkinApiKey()) {
            queue.submit(job);
            tasks.sync(() -> tellQueued(owner));
        } else if (configuration.settings().verbose()) {
            logger.info("Not rendering the outfit of " + owner + ": config.yml has no MineSkin API key.");
        }
    }

    private void applyLater(UUID owner, SignedTexture texture) {
        tasks.sync(() -> applier.apply(owner, texture));
    }

    private void tellQueued(UUID owner) {
        Player online = server.getPlayer(owner);
        if (online != null) {
            online.sendMessage(configuration.messages().get("render.queued"));
        }
    }
}
