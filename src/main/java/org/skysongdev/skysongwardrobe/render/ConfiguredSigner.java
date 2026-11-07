package org.skysongdev.skysongwardrobe.render;

import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Signs with the MineSkin API key config.yml has right now, so {@code /sw reload} picks up a new
 * key without a restart. The signer for a key is built once and reused until the key changes.
 */
public final class ConfiguredSigner implements SkinSigner {
    private final Supplier<String> apiKey;
    private final Function<String, SkinSigner> signerForKey;
    private String builtFor;
    private SkinSigner built;

    /**
     * @param apiKey       the key in config.yml, blank when none is set
     * @param signerForKey builds the signer for a key
     */
    public ConfiguredSigner(Supplier<String> apiKey, Function<String, SkinSigner> signerForKey) {
        this.apiKey = apiKey;
        this.signerForKey = signerForKey;
    }

    @Override
    public SignResult sign(BufferedImage skin, ArmModel armModel) {
        String key = apiKey.get();
        if (key.isBlank()) {
            return new SignResult.Failed("config.yml has no MineSkin API key");
        }
        return signer(key).sign(skin, armModel);
    }

    private synchronized SkinSigner signer(String key) {
        if (!key.equals(builtFor)) {
            built = signerForKey.apply(key);
            builtFor = key;
        }
        return built;
    }
}
