package org.skysongdev.skysongwardrobe.mannequin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinSkinsTest {
    @TempDir
    Path folder;
    private final List<String> warnings = new ArrayList<>();
    private final Logger logger = Logger.getAnonymousLogger();

    @BeforeEach
    void setUp() {
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    private void png(String name, int width, int height, int rgb) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, rgb);
        ImageIO.write(image, "png", folder.resolve(name).toFile());
    }

    @Test
    void namesEachSkinAfterItsFile() throws IOException {
        png("stone.png", 64, 64, 0xFF112233);
        png("oak_wood.png", 64, 64, 0xFF445566);

        MannequinSkins skins = MannequinSkins.load(folder, logger);

        assertEquals(Set.of("stone", "oak_wood"), skins.ids());
        assertEquals(0xFF112233, skins.image("stone").orElseThrow().getRGB(0, 0));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void matchesIdsLowercased() throws IOException {
        png("stone.png", 64, 64, 0);

        MannequinSkins skins = MannequinSkins.load(folder, logger);

        assertTrue(skins.has("Stone"));
        assertTrue(skins.image("STONE").isPresent());
        assertFalse(skins.has("marble"));
        assertFalse(skins.has(null));
    }

    @Test
    void skipsFilesThatAreNotSkinsWithAWarning() throws IOException {
        png("small.png", 64, 32, 0);
        png("Bad Name.png", 64, 64, 0);
        Files.writeString(folder.resolve("broken.png"), "not a png");
        Files.writeString(folder.resolve("notes.txt"), "ignored without a warning");

        MannequinSkins skins = MannequinSkins.load(folder, logger);

        assertTrue(skins.ids().isEmpty());
        assertEquals(3, warnings.size());
    }

    @Test
    void createsAMissingFolder() {
        Path missing = folder.resolve("mannequin-skins");

        MannequinSkins skins = MannequinSkins.load(missing, logger);

        assertTrue(Files.isDirectory(missing));
        assertTrue(skins.ids().isEmpty());
    }

    @Test
    void handsOutCopiesSoDrawingCannotChangeTheSet() throws IOException {
        png("stone.png", 64, 64, 0xFF112233);
        MannequinSkins skins = MannequinSkins.load(folder, logger);

        BufferedImage first = skins.image("stone").orElseThrow();
        first.setRGB(0, 0, 0xFFFFFFFF);

        assertNotSame(first, skins.image("stone").orElseThrow());
        assertEquals(0xFF112233, skins.image("stone").orElseThrow().getRGB(0, 0));
    }
}
