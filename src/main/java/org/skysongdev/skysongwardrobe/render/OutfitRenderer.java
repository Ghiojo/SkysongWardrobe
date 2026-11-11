package org.skysongdev.skysongwardrobe.render;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The part of a render every wearer shares: draws the worn layers over a base image, then finds
 * the result in the texture cache or queues it for MineSkin. {@link #read} takes the item tags on
 * the main thread; {@link #render} reads layer files, so call it off the main thread.
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

    private final Supplier<ItemTagGateway> tags;
    private final LayerResolver layers;
    private final TextureCache cache;
    private final RenderQueue queue;
    private final BooleanSupplier hasApiKey;
    private final List<RenderListener> observers = new CopyOnWriteArrayList<>();

    /** @param tags looked up per read, since the SkysongItems API is only there once it is ready */
    public OutfitRenderer(Supplier<ItemTagGateway> tags, LayerResolver layers, TextureCache cache, RenderQueue queue,
                          BooleanSupplier hasApiKey) {
        this.tags = tags;
        this.layers = layers;
        this.cache = cache;
        this.queue = queue;
        this.hasApiKey = hasApiKey;
    }

    /**
     * Tells {@code observer} of every outfit that is {@link Outcome.Ready ready} at once, already
     * signed, as {@link RenderListener#signed}; outfits that wait for MineSkin reach it through
     * the render queue instead. Called on the rendering thread.
     */
    public OutfitRenderer observe(RenderListener observer) {
        observers.add(observer);
        return this;
    }

    /** Reads what the worn items draw, for {@link #render}. Main thread only. Nothing worn needs no tags. */
    public List<ItemLayers> read(List<ItemStack> worn) {
        return worn.isEmpty() ? List.of() : ItemLayers.read(tags.get(), worn);
    }

    /**
     * Renders the worn items over {@code base}.
     *
     * @param bare the texture that already shows {@code base} alone, used as-is when nothing
     *             worn has a layer; empty when the bare base must be signed too
     */
    public Outcome render(UUID wearer, WearerKind kind, BufferedImage base, ArmModel arms,
                          Optional<SignedTexture> bare, List<ItemLayers> worn) {
        List<Layer> drawn = layers.layers(worn);
        if (drawn.isEmpty() && bare.isPresent()) {
            return ready(wearer, kind, bare.get());
        }
        RenderJob job = RenderJob.of(wearer, kind, SkinComposer.compose(base, drawn), arms);
        Optional<SignedTexture> known = cache.find(job.hash());
        if (known.isPresent()) {
            return ready(wearer, kind, known.get());
        }
        if (!hasApiKey.getAsBoolean()) {
            return new Outcome.NoApiKey();
        }
        queue.submit(job);
        return new Outcome.Queued();
    }

    private Outcome ready(UUID wearer, WearerKind kind, SignedTexture texture) {
        observers.forEach(observer -> observer.signed(wearer, kind, texture));
        return new Outcome.Ready(texture);
    }
}
