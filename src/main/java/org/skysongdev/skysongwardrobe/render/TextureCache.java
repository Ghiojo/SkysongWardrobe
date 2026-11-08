package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Every skin this plugin had signed, by the SHA-256 of its final image. An outfit worn before
 * is applied from here without calling MineSkin. It also tells which textures this plugin
 * applied, so a player's current skin can be told apart from a new base.
 * <p>
 * Where the skins are kept is up to the implementation, such as {@link YamlTextureCache}.
 * Implementations may block on disk or a database, so call them off the main thread.
 */
public interface TextureCache {

    /** The cache key of a composed skin: the SHA-256 of its pixels, in hex. */
    static String hash(BufferedImage image) {
        ByteBuffer pixels = ByteBuffer.allocate(image.getWidth() * image.getHeight() * Integer.BYTES);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.putInt(image.getRGB(x, y));
            }
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels.array()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java runtime has SHA-256", impossible);
        }
    }

    /** The signed skin for the image with this {@link #hash}, if it was signed before. */
    Optional<SignedTexture> find(String hash);

    /** Whether this plugin applied the texture, as opposed to Mojang or another skin plugin. */
    boolean isOwn(SignedTexture texture);

    /** Stores a newly signed skin. A hash stored again keeps the newest texture. */
    void put(String hash, SignedTexture texture) throws IOException;
}
