package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.skysongdev.skysongwardrobe.items.LayerKind;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.galleryWith;
import static org.skysongdev.skysongwardrobe.commands.GalleryCommandFixture.nextActionBar;

class ApplyCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock alice;
    private PlayerMock bob;
    private final FakeItemTags tags = new FakeItemTags().type(Material.LEATHER_CHESTPLATE, "coat");
    private CommandRunner commands;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        server.getScheduler().waitAsyncTasksFinished();
        alice.addAttachment(plugin, "skysongwardrobe.apply", true);
        galleryWith(plugin, alice, "cape");
        galleryWith(plugin, bob, "hood");
        alice.getInventory().setItemInMainHand(new ItemStack(Material.LEATHER_CHESTPLATE));
        commands = new CommandRunner(
                new ApplyCommand(plugin.galleries(), () -> tags, plugin.configuration(), server),
                new UnapplyCommand(() -> tags, plugin.configuration()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void appliesYourOwnSkinAsUuidColonId() throws CommandSyntaxException {
        commands.run(alice, "apply Cape regular");

        assertEquals(Optional.of(alice.getUniqueId() + ":cape"), tags.layer(null, LayerKind.REGULAR));
        assertEquals("[Wardrobe] Applied Alice:cape to the item in your hand.", nextActionBar(alice));
    }

    @Test
    void anotherPlayersSkinNeedsOther() throws CommandSyntaxException {
        commands.run(alice, "apply Bob:hood dyed");
        assertEquals("[Wardrobe] You can only apply skins from your own gallery.", nextActionBar(alice));
        assertEquals(Optional.empty(), tags.layer(null, LayerKind.DYABLE));

        alice.addAttachment(plugin, "skysongwardrobe.apply.other", true);
        commands.run(alice, "apply Bob:hood dyed");
        assertEquals(Optional.of(bob.getUniqueId() + ":hood"), tags.layer(null, LayerKind.DYABLE));
    }

    @Test
    void namingYourselfNeedsNoOther() throws CommandSyntaxException {
        commands.run(alice, "apply Alice:cape regular");

        assertEquals(Optional.of(alice.getUniqueId() + ":cape"), tags.layer(null, LayerKind.REGULAR));
    }

    @Test
    void refusesMissingEntriesPlayersLayersAndPlainItems() throws CommandSyntaxException {
        commands.run(alice, "apply scarf regular");
        assertEquals("[Wardrobe] Alice's gallery has no scarf.", nextActionBar(alice));

        commands.run(alice, "apply Nobody:cape regular");
        assertEquals("[Wardrobe] No player called Nobody has played here.", nextActionBar(alice));

        commands.run(alice, "apply cape glowing");
        assertEquals("[Wardrobe] The layer must be regular or dyed.", nextActionBar(alice));

        alice.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        commands.run(alice, "apply cape regular");
        assertEquals("[Wardrobe] Hold a SkysongItems item in your main hand.", nextActionBar(alice));
        assertEquals(Optional.empty(), tags.layer(null, LayerKind.REGULAR));
    }

    @Test
    void unapplyRemovesTheLayer() throws CommandSyntaxException {
        commands.run(alice, "apply cape regular");
        nextActionBar(alice);

        commands.run(alice, "unapply regular");
        assertEquals("[Wardrobe] Removed that layer from the item in your hand.", nextActionBar(alice));
        commands.run(alice, "unapply regular");
        assertEquals("[Wardrobe] The item in your hand has no such layer.", nextActionBar(alice));
    }

    @Test
    void suggestsIdsThenLayers() {
        assertEquals(List.of("cape"), commands.suggest(alice, "apply c"));
        assertEquals(List.of("dyed", "regular"), commands.suggest(alice, "apply cape ").stream().sorted().toList());
        assertEquals(List.of("dyed", "regular"), commands.suggest(alice, "unapply ").stream().sorted().toList());
    }

    @Test
    void needsAnApplyPermission() {
        assertThrows(CommandSyntaxException.class, () -> commands.run(bob, "apply hood regular"));
        assertThrows(CommandSyntaxException.class, () -> commands.run(bob, "unapply regular"));
    }
}
