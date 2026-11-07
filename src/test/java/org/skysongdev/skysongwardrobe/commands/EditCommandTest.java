package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.galleryWith;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.nextMessage;

class EditCommandTest {
    private static final SkinId CAPE = new SkinId("cape");

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock player;
    private PlayerGallery gallery;
    private final SkinUploader uploader = mock(SkinUploader.class);
    private CommandRunner commands;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer("Alice");
        server.getScheduler().waitAsyncTasksFinished();
        player.addAttachment(plugin, "skysongwardrobe.gallery", true);
        gallery = galleryWith(plugin, player, "cape", "hood");
        commands = new CommandRunner(new EditCommand(plugin.galleries(), uploader, plugin.configuration(),
                Logger.getAnonymousLogger()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void renamesWithSpaces() throws CommandSyntaxException {
        commands.run(player, "edit Cape name Red Winter Cape");

        assertEquals("Red Winter Cape", gallery.entry(CAPE).orElseThrow().name());
        assertEquals("[Wardrobe] cape is now called Red Winter Cape.", nextMessage(player));
    }

    @Test
    void changesTheIcon() throws CommandSyntaxException {
        commands.run(player, "edit cape icon red_banner");

        assertEquals(Optional.of(Material.RED_BANNER), gallery.entry(CAPE).orElseThrow().icon());
        assertEquals("[Wardrobe] cape now shows as RED_BANNER.", nextMessage(player));
    }

    @Test
    void refusesAnIconThatIsNotAnItem() throws CommandSyntaxException {
        commands.run(player, "edit cape icon nonsense");

        assertEquals(Optional.empty(), gallery.entry(CAPE).orElseThrow().icon());
        assertEquals("[Wardrobe] nonsense is not an item.", nextMessage(player));
    }

    @Test
    void unknownIdIsReported() throws CommandSyntaxException {
        commands.run(player, "edit scarf name Scarf");
        assertEquals("[Wardrobe] Your gallery has no scarf.", nextMessage(player));

        commands.run(player, "edit bad-id name Scarf");
        assertEquals("[Wardrobe] Your gallery has no bad-id.", nextMessage(player));
    }

    @Test
    void newLinkGoesThroughTheUploader() throws CommandSyntaxException {
        commands.run(player, "edit cape link https://example.com/new.png");

        verify(uploader).relink(player, CAPE, URI.create("https://example.com/new.png"));
    }

    @Test
    void refusesALinkThatIsNotHttp() throws CommandSyntaxException {
        commands.run(player, "edit cape link file:///etc/passwd");

        verify(uploader, never()).relink(any(), any(), any());
        assertTrue(nextMessage(player).contains("is not an http or https link"));
    }

    @Test
    void suggestsOwnIdsFieldsAndIcons() {
        assertEquals(List.of("cape"), commands.suggest(player, "edit c"));
        assertEquals(List.of("icon", "link", "name"), commands.suggest(player, "edit cape ").stream().sorted().toList());
        assertTrue(commands.suggest(player, "edit cape icon red_ban").contains("red_banner"));
    }
}
