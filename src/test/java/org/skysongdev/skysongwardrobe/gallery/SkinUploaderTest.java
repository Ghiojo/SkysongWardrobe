package org.skysongdev.skysongwardrobe.gallery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.download.DownloadFailure;
import org.skysongdev.skysongwardrobe.download.DownloadResult;
import org.skysongdev.skysongwardrobe.prompt.ConfirmationPrompt;
import org.skysongdev.skysongwardrobe.prompt.PromptText;
import org.skysongdev.skysongwardrobe.scheduling.ImmediateTaskRunner;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinUploaderTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final byte[] PNG = {1, 2, 3};
    private static final SkinId CAPE = new SkinId("cape");

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock player;
    private final List<String> links = new ArrayList<>();
    private DownloadResult nextDownload = new DownloadResult.Success(PNG);
    private boolean agree = true;
    private PromptText shown;
    private SkinUploader uploader;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer("Alice");
        server.getScheduler().waitAsyncTasksFinished();
        ConfirmationPrompt prompt = (target, text, onAgree, onDecline) -> {
            shown = text;
            (agree ? onAgree : onDecline).run();
        };
        uploader = new SkinUploader(plugin.galleries(), () -> link -> {
            links.add(link);
            return nextDownload;
        }, prompt, plugin.configuration(), new ImmediateTaskRunner(), Clock.fixed(NOW, ZoneOffset.UTC),
                Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void agreeDownloadsAndSaves() throws IOException {
        uploader.start(player, request(CAPE, "Red Cape"));

        assertTrue(plain(shown.body()).contains("Red Cape"));
        assertEquals(List.of("https://example.com/cape.png"), links);
        PlayerGallery gallery = plugin.galleries().existing(player.getUniqueId()).orElseThrow();
        SkinEntry saved = gallery.entry(CAPE).orElseThrow();
        assertEquals("Red Cape", saved.name());
        assertEquals(Optional.of(Material.RED_BANNER), saved.icon());
        assertEquals(NOW, saved.uploadedAt());
        assertArrayEquals(PNG, Files.readAllBytes(gallery.imageFile(CAPE)));
        assertEquals("[Wardrobe] Downloading cape...", nextMessage());
        assertEquals("[Wardrobe] Saved Red Cape as cape in your gallery.", nextMessage());
    }

    @Test
    void declineDownloadsNothing() {
        agree = false;

        uploader.start(player, request(CAPE, "cape"));

        assertTrue(links.isEmpty());
        assertTrue(plugin.galleries().existing(player.getUniqueId()).flatMap(g -> g.entry(CAPE)).isEmpty());
        assertEquals("[Wardrobe] Upload of cape cancelled.", nextMessage());
    }

    @Test
    void takenIdIsRefusedBeforeAsking() {
        uploader.start(player, request(CAPE, "cape"));
        player.nextComponentMessage();
        player.nextComponentMessage();
        shown = null;

        uploader.start(player, request(CAPE, "Other"));

        assertNull(shown);
        assertEquals(1, links.size());
        assertTrue(nextMessage().contains("already has cape"));
    }

    @Test
    void downloadFailureSavesNothingAndSaysWhy() {
        nextDownload = DownloadResult.failed(DownloadFailure.WRONG_SIZE);

        uploader.start(player, request(CAPE, "cape"));

        player.nextComponentMessage();
        assertEquals("[Wardrobe] Skins must be 64x64 pixels.", nextMessage());
        assertTrue(plugin.galleries().existing(player.getUniqueId()).flatMap(g -> g.entry(CAPE)).isEmpty());
    }

    @Test
    void everyFailureHasAMessage() {
        for (DownloadFailure failure : DownloadFailure.values()) {
            nextDownload = DownloadResult.failed(failure);
            uploader.start(player, request(CAPE, "cape"));
            player.nextComponentMessage();
            String message = nextMessage();
            assertFalse(message.contains("upload.failed"), failure + " has no text: " + message);
        }
    }

    private String nextMessage() {
        return plain(player.nextComponentMessage());
    }

    private static UploadRequest request(SkinId id, String name) {
        return new UploadRequest(id, URI.create("https://example.com/cape.png"), Optional.of(Material.RED_BANNER), name);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
