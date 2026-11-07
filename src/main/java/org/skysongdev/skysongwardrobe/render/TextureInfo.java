package org.skysongdev.skysongwardrobe.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * What a signed {@code textures} value says: where the skin image is and which arm model it
 * uses. The value is Base64 JSON: {@code {"textures":{"SKIN":{"url":..,"metadata":{"model":"slim"}}}}}.
 */
public record TextureInfo(URI skinUrl, ArmModel armModel) {

    /** Reads a value; empty if it has no skin or isn't a textures value. */
    public static Optional<TextureInfo> parse(String value) {
        try {
            JsonElement root = JsonParser.parseString(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8));
            JsonObject skin = root.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) {
                return Optional.empty();
            }
            JsonObject metadata = skin.getAsJsonObject("metadata");
            boolean slim = metadata != null && metadata.has("model") && "slim".equals(metadata.get("model").getAsString());
            return Optional.of(new TextureInfo(URI.create(skin.get("url").getAsString()), slim ? ArmModel.SLIM : ArmModel.CLASSIC));
        } catch (IllegalArgumentException | IllegalStateException | ClassCastException | NullPointerException
                 | JsonParseException notATexture) {
            return Optional.empty();
        }
    }
}
