package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Optional;

/** Downloads an image. Blocks, so call it off the main thread. */
@FunctionalInterface
public interface ImageFetcher {
    /** The image, or empty if it can't be downloaded or decoded. */
    Optional<BufferedImage> fetch(URI uri);
}
