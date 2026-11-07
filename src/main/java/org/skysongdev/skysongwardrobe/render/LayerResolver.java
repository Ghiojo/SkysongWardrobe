package org.skysongdev.skysongwardrobe.render;

import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.SkinReference;
import org.skysongdev.skysongwardrobe.items.LayerKind;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * Turns the worn items' {@link ItemLayers} into the layers to draw, in order: for each item
 * (lowest slot priority first) its dyable layer, tinted by its dye colour, then its regular layer. A layer whose
 * image is missing or broken is skipped silently, and only logged when config.yml has
 * {@code verbose: true}. A deleted entry's image stays on disk, so it still renders.
 */
public final class LayerResolver {
    private final GalleryRepository galleries;
    private final BooleanSupplier verbose;
    private final Logger logger;

    public LayerResolver(GalleryRepository galleries, BooleanSupplier verbose, Logger logger) {
        this.galleries = galleries;
        this.verbose = verbose;
        this.logger = logger;
    }

    /** Reads image files, so call it off the main thread. */
    public List<Layer> layers(List<ItemLayers> worn) {
        List<Layer> layers = new ArrayList<>();
        for (ItemLayers item : worn) {
            image(item.dyable(), LayerKind.DYABLE).ifPresent(image -> layers.add(new Layer(image, item.dye())));
            image(item.regular(), LayerKind.REGULAR).ifPresent(image -> layers.add(Layer.untinted(image)));
        }
        return layers;
    }

    private Optional<BufferedImage> image(Optional<String> stored, LayerKind kind) {
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        Optional<SkinReference> reference = SkinReference.parse(stored.get());
        if (reference.isEmpty()) {
            return skip(kind.tagId() + " value " + stored.get() + " is not uuid:id.");
        }
        Optional<Path> file = galleries.existing(reference.get().owner())
                .map(gallery -> gallery.imageFile(reference.get().id()))
                .filter(Files::isRegularFile);
        if (file.isEmpty()) {
            return skip("No image for " + stored.get() + ".");
        }
        try {
            BufferedImage image = ImageIO.read(file.get().toFile());
            if (image == null || image.getWidth() != SkinComposer.SIZE || image.getHeight() != SkinComposer.SIZE) {
                return skip(file.get() + " is not a 64x64 image.");
            }
            return Optional.of(image);
        } catch (IOException broken) {
            return skip(file.get() + " can't be read: " + broken.getMessage());
        }
    }

    private Optional<BufferedImage> skip(String why) {
        if (verbose.getAsBoolean()) {
            logger.info("Skipped a wardrobe layer: " + why);
        }
        return Optional.empty();
    }
}
