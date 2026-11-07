package org.skysongdev.skysongwardrobe.render;

import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The part of a render every wearer shares: draws the worn layers over a base image, then finds
 * the result in the texture cache or queues it for MineSkin. Reads layer files, so call it off
 * the main thread.
 */
public final class OutfitRenderer {
    /** How a render ended. */
    public sealed interface Outcome {
        /** The texture to show now. */
        record Ready(SignedTexture texture) implements Outcome {
        }

        /** A job is waiting in the render queue; the listener hears when it is signed. */
        record Queued() implements Outcome {
        }

        /** The look is new but config.yml has no MineSkin API key, so it can't be signed. */
        record NoApiKey() implements Outcome {
        }
    }

    private final Supplier<LayerResolver> layers;
    private final TextureCache cache;
    private final RenderQueue queue;
    private final BooleanSupplier hasApiKey;

    public OutfitRenderer(Supplier<LayerResolver> layers, TextureCache cache, RenderQueue queue, BooleanSupplier hasApiKey) {
        this.layers = layers;
        this.cache = cache;
        this.queue = queue;
        this.hasApiKey = hasApiKey;
    }

    /**
     * Renders the worn items over {@code base}.
     *
     * @param bare the texture that already shows {@code base} alone, used as-is when nothing
     *             worn has a layer; empty when the bare base must be signed too
     */
    public Outcome render(UUID wearer, WearerKind kind, BufferedImage base, ArmModel arms,
                          Optional<SignedTexture> bare, List<ItemStack> worn) {
        List<Layer> drawn = layers.get().layers(worn);
        if (drawn.isEmpty() && bare.isPresent()) {
            return new Outcome.Ready(bare.get());
        }
        RenderJob job = RenderJob.of(wearer, kind, SkinComposer.compose(base, drawn), arms);
        Optional<SignedTexture> known = cache.find(job.hash());
        if (known.isPresent()) {
            return new Outcome.Ready(known.get());
        }
        if (!hasApiKey.getAsBoolean()) {
            return new Outcome.NoApiKey();
        }
        queue.submit(job);
        return new Outcome.Queued();
    }
}
