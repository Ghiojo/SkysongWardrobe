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
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.menu.GalleryMenu;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.galleryWith;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.nextMessage;

class GalleryCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock alice;
    private PlayerMock bob;
    private CommandRunner commands;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        server.getScheduler().waitAsyncTasksFinished();
        alice.addAttachment(plugin, "skysongwardrobe.gallery", true);
        galleryWith(plugin, alice, "cape");
        galleryWith(plugin, bob, "hood", "scarf");
        commands = new CommandRunner(new GalleryCommand(plugin.galleries(), plugin.configuration(), server));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void opensYourOwnGallery() throws CommandSyntaxException {
        commands.run(alice, "gallery");

        assertInstanceOf(GalleryMenu.class, alice.getOpenInventory().getTopInventory().getHolder(false));
        assertEquals(Material.PAPER, alice.getOpenInventory().getTopInventory().getItem(0).getType());
        assertNull(alice.getOpenInventory().getTopInventory().getItem(1));
    }

    @Test
    void anotherPlayersGalleryNeedsOther() throws CommandSyntaxException {
        assertThrows(CommandSyntaxException.class, () -> commands.run(alice, "gallery Bob"));

        alice.addAttachment(plugin, "skysongwardrobe.gallery.other", true);
        commands.run(alice, "gallery Bob");

        assertEquals("Bob's gallery", PlainTextComponentSerializer.plainText().serialize(alice.getOpenInventory().title()));
        assertNotNull(alice.getOpenInventory().getTopInventory().getItem(1));
    }

    @Test
    void offlinePlayersGalleryOpensFromDisk() throws CommandSyntaxException {
        alice.addAttachment(plugin, "skysongwardrobe.gallery.other", true);
        bob.disconnect();

        commands.run(alice, "gallery Bob");

        assertEquals(2, alice.getOpenInventory().getTopInventory().all(Material.PAPER).size());
    }

    @Test
    void unknownPlayerIsReported() throws CommandSyntaxException {
        alice.addAttachment(plugin, "skysongwardrobe.gallery.other", true);

        commands.run(alice, "gallery Nobody");

        assertEquals("[Wardrobe] No player called Nobody has played here.", nextMessage(alice));
    }

    @Test
    void needsAGalleryPermission() {
        assertThrows(CommandSyntaxException.class, () -> commands.run(bob, "gallery"));
    }
}
