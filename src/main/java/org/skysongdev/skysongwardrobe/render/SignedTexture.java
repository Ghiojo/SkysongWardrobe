package org.skysongdev.skysongwardrobe.render;

import java.util.Objects;

/** A {@code textures} profile property as Mojang or MineSkin signed it. */
public record SignedTexture(String value, String signature) {
    public SignedTexture {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(signature, "signature");
    }
}
