package org.skysongdev.skysongwardrobe.render;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * texture-cache.yml: every skin this plugin had signed, by the SHA-256 of its final image.
 * An outfit worn before is applied from here without calling MineSkin. It also tells which
 * textures this plugin applied, so a player's current skin can be told apart from a new base.
 */
public final class TextureCache {
    private final Path file;
    private final Map<String, SignedTexture> byHash = new HashMap<>();
    private final Set<String> ownValues = new HashSet<>();

    private TextureCache(Path file) {
        this.file = file;
    }

    public static TextureCache load(Path file) {
        TextureCache cache = new TextureCache(file);
        if (Files.isRegularFile(file)) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            for (String hash : yaml.getKeys(false)) {
                ConfigurationSection entry = yaml.getConfigurationSection(hash);
                if (entry != null && entry.isString("value") && entry.isString("signature")) {
                    cache.remember(hash, new SignedTexture(entry.getString("value"), entry.getString("signature")));
                }
            }
        }
        return cache;
    }

    /** The cache key of a composed skin: the SHA-256 of its pixels, in hex. */
    public static String hash(BufferedImage image) {
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

    public synchronized Optional<SignedTexture> find(String hash) {
        return Optional.ofNullable(byHash.get(hash));
    }

    /** Whether this plugin applied the texture, as opposed to Mojang or another skin plugin. */
    public synchronized boolean isOwn(SignedTexture texture) {
        return ownValues.contains(texture.value());
    }

    /** Stores a newly signed skin and writes the file. */
    public synchronized void put(String hash, SignedTexture texture) throws IOException {
        remember(hash, texture);
        YamlConfiguration yaml = new YamlConfiguration();
        byHash.forEach((key, signed) -> {
            yaml.set(key + ".value", signed.value());
            yaml.set(key + ".signature", signed.signature());
        });
        Files.createDirectories(file.getParent());
        yaml.save(file.toFile());
    }

    private void remember(String hash, SignedTexture texture) {
        byHash.put(hash, texture);
        ownValues.add(texture.value());
    }
}
