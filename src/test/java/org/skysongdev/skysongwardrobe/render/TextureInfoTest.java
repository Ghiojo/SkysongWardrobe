package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureInfoTest {
    private static final String URL = "http://textures.minecraft.net/texture/abc";

    @Test
    void readsUrlAndClassicModel() {
        assertEquals(Optional.of(new TextureInfo(URI.create(URL), ArmModel.CLASSIC)),
                TextureInfo.parse(encode("{\"textures\":{\"SKIN\":{\"url\":\"" + URL + "\"}}}")));
    }

    @Test
    void readsSlimModel() {
        assertEquals(ArmModel.SLIM, TextureInfo.parse(encode(
                "{\"textures\":{\"SKIN\":{\"url\":\"" + URL + "\",\"metadata\":{\"model\":\"slim\"}}}}")).orElseThrow().armModel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"textures\":{}}", "{\"textures\":{\"CAPE\":{\"url\":\"x\"}}}", "[1]", "nope"})
    void anythingElseIsEmpty(String json) {
        assertTrue(TextureInfo.parse(encode(json)).isEmpty());
    }

    @Test
    void notBase64IsEmpty() {
        assertTrue(TextureInfo.parse("%%%").isEmpty());
    }

    static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
