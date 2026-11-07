package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.gallery.DeletedSkinArchive;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.galleryWith;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.nextMessage;

class DeleteCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock player;
    private PlayerGallery gallery;
    private Path deleted;
    private CommandRunner commands;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer("Alice");
        server.getScheduler().waitAsyncTasksFinished();
        player.addAttachment(plugin, "skysongwardrobe.gallery", true);
        gallery = galleryWith(plugin, player, "cape");
        deleted = plugin.getDataFolder().toPath().resolve("deleted");
        commands = new CommandRunner(new DeleteCommand(plugin.galleries(), new DeletedSkinArchive(deleted),
                plugin.configuration(), Logger.getAnonymousLogger()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void copiesToDeletedAndRemovesTheEntry() throws CommandSyntaxException {
        commands.run(player, "del Cape");

        assertTrue(gallery.entry(new SkinId("cape")).isEmpty());
        assertTrue(Files.isRegularFile(deleted.resolve("Alice-cape.png")));
        assertTrue(nextMessage(player).startsWith("[Wardrobe] Deleted cape from your gallery."));
    }

    @Test
    void unknownIdIsReported() throws CommandSyntaxException {
        commands.run(player, "del hood");

        assertEquals("[Wardrobe] Your gallery has no hood.", nextMessage(player));
        assertTrue(Files.notExists(deleted));
    }

    @Test
    void suggestsOwnIdsAndNeedsAGalleryPermission() {
        assertEquals(List.of("cape"), commands.suggest(player, "del "));
        assertThrows(CommandSyntaxException.class, () -> commands.run(server.addPlayer(), "del cape"));
    }
}
