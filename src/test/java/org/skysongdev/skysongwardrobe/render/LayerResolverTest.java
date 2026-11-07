package org.skysongdev.skysongwardrobe.render;

import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.gallery.GalleryFolders;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.items.LayerKind;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerResolverTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final Color RED = Color.fromRGB(0xFF0000);

    @TempDir
    Path root;
    private GalleryRepository galleries;
    private PlayerGallery gallery;
    private final List<String> logged = new ArrayList<>();
    private boolean verbose;
    private LayerResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        GalleryFolders folders = new GalleryFolders(root, player -> Optional.empty(), logger);
        folders.scan();
        galleries = new GalleryRepository(folders, logger);
        gallery = galleries.claim(OWNER, "Alice");
        add("cape", 64);
        add("trim", 64);
        add("small", 32);
        resolver = new LayerResolver(galleries, () -> verbose, logger);
    }

    @Test
    void dyableLayerComesFirstTintedThenTheRegularOne() {
        ItemLayers coat = item(Map.of(LayerKind.REGULAR, ref("cape"), LayerKind.DYABLE, ref("trim")));

        List<Layer> result = resolver.layers(List.of(coat));

        assertEquals(2, result.size());
        assertEquals(RED, result.get(0).tint());
        assertEquals(Color.WHITE, result.get(1).tint());
    }

    @Test
    void itemsKeepTheirOrder() {
        ItemLayers boots = item(Map.of(LayerKind.REGULAR, ref("trim")));
        ItemLayers coat = item(Map.of(LayerKind.REGULAR, ref("cape")));

        List<Layer> result = resolver.layers(List.of(boots, coat));

        // Each test image marks its first pixel with the order it was added in: cape 0, trim 1.
        assertEquals(List.of(0xFF000001, 0xFF000000), result.stream().map(layer -> layer.image().getRGB(0, 0)).toList());
    }

    @Test
    void deletedEntriesStillRenderFromTheirFile() throws IOException {
        gallery.remove(new SkinId("cape"));

        assertEquals(1, resolver.layers(List.of(item(Map.of(LayerKind.REGULAR, ref("cape"))))).size());
    }

    @Test
    void brokenLayersAreSkippedQuietlyUnlessVerbose() {
        ItemLayers broken = item(Map.of(LayerKind.REGULAR, ref("missing"), LayerKind.DYABLE, "not a reference"));
        ItemLayers small = item(Map.of(LayerKind.REGULAR, ref("small")));
        ItemLayers stranger = item(Map.of(LayerKind.REGULAR, UUID.randomUUID() + ":cape"));

        assertTrue(resolver.layers(List.of(broken, small, stranger)).isEmpty());
        assertTrue(logged.isEmpty());

        verbose = true;
        resolver.layers(List.of(broken, small, stranger));
        assertEquals(4, logged.size());
    }

    private void add(String id, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF000000 | gallery.entries().size());
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        gallery.add(new SkinEntry(new SkinId(id), id, Optional.empty(), URI.create("https://e.com/" + id), Instant.EPOCH),
                png.toByteArray());
        assertTrue(Files.isRegularFile(gallery.imageFile(new SkinId(id))));
    }

    private static ItemLayers item(Map<LayerKind, String> values) {
        return new ItemLayers(Optional.ofNullable(values.get(LayerKind.DYABLE)), RED,
                Optional.ofNullable(values.get(LayerKind.REGULAR)));
    }

    private static String ref(String id) {
        return OWNER + ":" + id;
    }
}
