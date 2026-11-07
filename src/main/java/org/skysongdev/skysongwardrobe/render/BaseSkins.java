package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * Remembers the skin each player has under their outfit. The player's current texture becomes
 * the base unless this plugin applied it, so a skin from Mojang or from a plugin such as
 * SkinsRestorer is always built on. The base's arm model is kept for every render.
 */
public final class BaseSkins {
    private final TextureCache cache;
    private final ImageFetcher fetcher;
    private final BooleanSupplier verbose;
    private final Logger logger;
    private final Map<UUID, BaseSkin> bases = new ConcurrentHashMap<>();

    public BaseSkins(TextureCache cache, ImageFetcher fetcher, BooleanSupplier verbose, Logger logger) {
        this.cache = cache;
        this.fetcher = fetcher;
        this.verbose = verbose;
        this.logger = logger;
    }

    /**
     * The player's base skin, taking {@code current} (their profile's textures property right
     * now) as the new base when it isn't one of ours. Downloads the image, so call it off the
     * main thread. Empty when there is no usable base, such as on an offline-mode server.
     */
    public Optional<BaseSkin> resolve(UUID player, Optional<SignedTexture> current) {
        if (current.isPresent() && !cache.isOwn(current.get())) {
            BaseSkin known = bases.get(player);
            if (known != null && known.texture().equals(current.get())) {
                return Optional.of(known);
            }
            Optional<BaseSkin> fresh = download(current.get());
            fresh.ifPresent(base -> bases.put(player, base));
            return fresh;
        }
        return Optional.ofNullable(bases.get(player));
    }

    /** Forgets a player's base, for one who left; their next join brings their own skin back anyway. */
    public void forget(UUID player) {
        bases.remove(player);
    }

    private Optional<BaseSkin> download(SignedTexture texture) {
        Optional<TextureInfo> info = TextureInfo.parse(texture.value());
        if (info.isEmpty()) {
            return skip("the player's textures value has no skin.");
        }
        Optional<BufferedImage> image = fetcher.fetch(info.get().skinUrl()).flatMap(LegacySkin::toModern);
        if (image.isEmpty()) {
            return skip("the skin at " + info.get().skinUrl() + " could not be downloaded as a 64x64 or 64x32 image.");
        }
        return Optional.of(new BaseSkin(texture, info.get().armModel(), image.get()));
    }

    private Optional<BaseSkin> skip(String why) {
        if (verbose.getAsBoolean()) {
            logger.info("No base skin: " + why);
        }
        return Optional.empty();
    }
}
