package org.skysongdev.skysongwardrobe.mannequin;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinPlaceListenerTest {
    @TempDir
    Path folder;

    private PlayerMock player;
    private Block ground;
    private final List<Mannequin> placed = new ArrayList<>();
    private MannequinPlaceListener listener;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        player = server.addPlayer();
        ground = server.addSimpleWorld("world").getBlockAt(0, 64, 0);
        ground.setType(Material.STONE);
        MannequinPlacer placer = new MannequinPlacer(MannequinRepository.load(folder, Logger.getAnonymousLogger()),
                new MannequinMarker(plugin), (location, model) -> location.getWorld().spawn(location, Zombie.class),
                new OwnerAccess(() -> plugin.configuration().settings().mannequins()),
                () -> new FakeItemTags().spawner(Material.ARMOR_STAND, "oak", "classic"), plugin.configuration(),
                placed::add, Logger.getAnonymousLogger());
        listener = new MannequinPlaceListener(placer, plugin.configuration()::messages);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerInteractEvent click(Material item, BlockFace face) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(item), ground, face, EquipmentSlot.HAND);
    }

    @Test
    void aRefusedSpawnerClickIsUsedUpAndExplained() {
        PlayerInteractEvent event = click(Material.ARMOR_STAND, BlockFace.UP);

        listener.onInteract(event);

        assertTrue(event.isCancelled());
        assertEquals("[Wardrobe] You can't place mannequins.",
                PlainTextComponentSerializer.plainText().serialize(player.nextActionBar()));
    }

    @Test
    void otherItemsAreLeftAlone() {
        PlayerInteractEvent event = click(Material.STICK, BlockFace.UP);

        listener.onInteract(event);

        assertFalse(event.isCancelled());
        assertNull(player.nextActionBar());
    }

    @Test
    void onlyTheTopOfABlockPlaces() {
        PlayerInteractEvent event = click(Material.ARMOR_STAND, BlockFace.NORTH);

        listener.onInteract(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aClickAnotherPluginDeniedIsLeftAlone() {
        PlayerInteractEvent event = click(Material.ARMOR_STAND, BlockFace.UP);
        event.setUseInteractedBlock(Event.Result.DENY);

        listener.onInteract(event);

        assertNull(player.nextActionBar());
        assertTrue(placed.isEmpty());
    }
}
