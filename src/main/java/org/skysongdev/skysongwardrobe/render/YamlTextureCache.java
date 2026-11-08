package org.skysongdev.skysongwardrobe.render;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link TextureCache} in {@code texture-cache.yml}. Every skin is held in memory, and the
 * whole file is written again on each new skin, so it suits servers with a modest cache.
 */
public final class YamlTextureCache implements TextureCache {
    private final Path file;
    private final Map<String, SignedTexture> byHash = new HashMap<>();
    private final Set<String> ownValues = new HashSet<>();

    private YamlTextureCache(Path file) {
        this.file = file;
    }

    /** The cache saved in {@code file}; empty when the file doesn't exist yet. */
    public static YamlTextureCache load(Path file) {
        YamlTextureCache cache = new YamlTextureCache(file);
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

    @Override
    public synchronized Optional<SignedTexture> find(String hash) {
        return Optional.ofNullable(byHash.get(hash));
    }

    /** A copy of every stored skin by hash, for importing them into a database. */
    public synchronized Map<String, SignedTexture> entries() {
        return Map.copyOf(byHash);
    }

    @Override
    public synchronized boolean isOwn(SignedTexture texture) {
        return ownValues.contains(texture.value());
    }

    /** Stores a newly signed skin and writes the whole file again. */
    @Override
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
