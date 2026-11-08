package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.nextActionBar;

class WardrobeCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock alice;
    private PlayerMock bob;
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        alice.addAttachment(plugin, WardrobeCommand.PERMISSION, true);
        dispatcher.getRoot().addChild(new WardrobeCommand(plugin.wardrobes(), FakeItemTags::new,
                plugin.configuration(), server).tree());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void opensYourOwnWardrobe() throws CommandSyntaxException {
        run(alice, "wardrobe");

        WardrobeMenu menu = assertInstanceOf(WardrobeMenu.class, alice.getOpenInventory().getTopInventory().getHolder(false));
        assertEquals(alice.getUniqueId(), menu.wardrobe().owner());
    }

    @Test
    void needsThePermission() {
        assertThrows(CommandSyntaxException.class, () -> run(bob, "wardrobe"));
    }

    @Test
    void anotherPlayersWardrobeNeedsOtherAndWorksOffline() throws CommandSyntaxException, IOException {
        Wardrobe bobs = plugin.wardrobes().load(bob.getUniqueId());
        bobs.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        plugin.wardrobes().save(bobs);
        assertThrows(CommandSyntaxException.class, () -> run(alice, "wardrobe Bob"));

        alice.addAttachment(plugin, "skysongwardrobe.wardrobe.other", true);
        bob.disconnect();
        run(alice, "wardrobe Bob");

        assertEquals(Material.LEATHER_CHESTPLATE, alice.getOpenInventory().getTopInventory().getItem(10).getType());
    }

    @Test
    void unknownPlayerIsReported() throws CommandSyntaxException {
        alice.addAttachment(plugin, "skysongwardrobe.wardrobe.other", true);

        run(alice, "wardrobe Nobody");

        assertEquals("[Wardrobe] No player called Nobody has played here.", nextActionBar(alice));
    }

    @Test
    void brokenLayoutKeepsTheWardrobeClosed() throws IOException, CommandSyntaxException {
        Files.writeString(plugin.getDataFolder().toPath().resolve("wardrobe.yml"), "slots: {}\n");
        plugin.configuration().reload();

        run(alice, "wardrobe");

        assertEquals("[Wardrobe] The wardrobe is closed until staff fix wardrobe.yml.", nextActionBar(alice));
    }

    private void run(PlayerMock sender, String command) throws CommandSyntaxException {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        dispatcher.execute(command, source);
    }
}
