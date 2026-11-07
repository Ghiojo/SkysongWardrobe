package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;
import org.skysongdev.skysongwardrobe.gallery.UploadRequest;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UploadCommandTest {
    private static final String LINK = "https://example.com/cape.png";

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock player;
    private final SkinUploader uploader = mock(SkinUploader.class);
    private CommandRunner commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer();
        player.addAttachment(plugin, "skysongwardrobe.gallery", true);
        commands = new CommandRunner(new UploadCommand(uploader, plugin.configuration()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void idAndLinkAloneNameTheEntryAfterItsId() throws CommandSyntaxException {
        assertEquals(new UploadRequest(new SkinId("red_cape"), URI.create(LINK), Optional.empty(), "red_cape"),
                upload("Red_Cape " + LINK));
    }

    @Test
    void wordAfterTheLinkIsTheIconWhenItNamesAnItem() throws CommandSyntaxException {
        assertEquals(new UploadRequest(new SkinId("cape"), URI.create(LINK), Optional.of(Material.RED_BANNER), "Red Cape"),
                upload("cape " + LINK + " red_banner Red Cape"));
    }

    @Test
    void wordAfterTheLinkStartsTheNameOtherwise() throws CommandSyntaxException {
        assertEquals(new UploadRequest(new SkinId("cape"), URI.create(LINK), Optional.empty(), "Fancy Red Cape"),
                upload("cape " + LINK + " Fancy Red Cape"));
    }

    @Test
    void refusesBadIdsAndLinks() throws CommandSyntaxException {
        commands.run(player, "upload red-cape " + LINK);
        assertTrue(nextMessage().contains("red-cape can't be used"));

        commands.run(player, "upload cape ftp://example.com/cape.png");
        assertTrue(nextMessage().contains("is not an http or https link"));

        verify(uploader, never()).start(any(), any());
    }

    @Test
    void needsAGalleryPermissionAndAPlayer() {
        PlayerMock stranger = server.addPlayer();

        assertThrows(CommandSyntaxException.class, () -> commands.run(stranger, "upload cape " + LINK));
        assertThrows(CommandSyntaxException.class, () -> commands.run(server.getConsoleSender(), "upload cape " + LINK));
    }

    @Test
    void suggestsIconsAfterTheLink() {
        assertTrue(commands.suggest(player, "upload cape " + LINK + " red_ban").contains("red_banner"));
        assertTrue(commands.suggest(player, "upload cape " + LINK + " Red Cape").isEmpty());
    }

    private UploadRequest upload(String arguments) throws CommandSyntaxException {
        commands.run(player, "upload " + arguments);
        ArgumentCaptor<UploadRequest> request = ArgumentCaptor.forClass(UploadRequest.class);
        verify(uploader).start(any(), request.capture());
        return request.getValue();
    }

    private String nextMessage() {
        return PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
    }
}
