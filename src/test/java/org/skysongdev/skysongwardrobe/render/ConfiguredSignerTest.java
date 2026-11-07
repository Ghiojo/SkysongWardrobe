package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredSignerTest {
    private static final BufferedImage SKIN = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

    private final List<String> built = new ArrayList<>();
    private String key = "first";
    private final ConfiguredSigner signer = new ConfiguredSigner(() -> key, forKey -> {
        built.add(forKey);
        return (skin, armModel) -> new SignResult.Signed(new SignedTexture(forKey, "sig"));
    });

    @Test
    void signsWithTheConfiguredKey() {
        assertEquals(new SignResult.Signed(new SignedTexture("first", "sig")), signer.sign(SKIN, ArmModel.CLASSIC));
    }

    @Test
    void reusesTheSignerWhileTheKeyStaysTheSame() {
        signer.sign(SKIN, ArmModel.CLASSIC);
        signer.sign(SKIN, ArmModel.SLIM);

        assertEquals(List.of("first"), built);
    }

    @Test
    void aNewKeyAfterAReloadBuildsANewSigner() {
        signer.sign(SKIN, ArmModel.CLASSIC);
        key = "second";

        assertEquals(new SignResult.Signed(new SignedTexture("second", "sig")), signer.sign(SKIN, ArmModel.CLASSIC));
        assertEquals(List.of("first", "second"), built);
    }

    @Test
    void failsWithoutAKey() {
        key = " ";

        assertInstanceOf(SignResult.Failed.class, signer.sign(SKIN, ArmModel.CLASSIC));
        assertTrue(built.isEmpty());
    }
}
