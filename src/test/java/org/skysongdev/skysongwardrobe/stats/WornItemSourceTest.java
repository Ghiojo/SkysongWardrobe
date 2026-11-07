package org.skysongdev.skysongwardrobe.stats;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WornItemSourceTest {
    private static final WardrobeLayout LAYOUT = new WardrobeLayout("Wardrobe", Material.GRAY_STAINED_GLASS_PANE, " ",
            List.of(new SlotDefinition("coat", 10, Set.of("coat"), 30), new SlotDefinition("boots", 28, Set.of("boots"), 10)));

    @TempDir
    Path folder;
    private PlayerMock player;
    private WardrobeRepository wardrobes;
    private Optional<WardrobeLayout> layout = Optional.of(LAYOUT);
    private WornItemSource source;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        player = server.addPlayer();
        wardrobes = new WardrobeRepository(folder, Logger.getAnonymousLogger());
        source = new WornItemSource(wardrobes, () -> layout);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void wornItemsCount() {
        wardrobes.load(player.getUniqueId()).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        wardrobes.load(player.getUniqueId()).put("boots", new ItemStack(Material.LEATHER_BOOTS));

        assertEquals(List.of(Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE),
                source.items(player).stream().map(ItemStack::getType).toList());
    }

    @Test
    void anItemInASlotWardrobeYmlNoLongerListsDoesNotCount() {
        wardrobes.load(player.getUniqueId()).put("removed", new ItemStack(Material.DIAMOND));

        assertTrue(source.items(player).isEmpty());
    }

    @Test
    void nothingCountsWhileTheLayoutIsBroken() {
        wardrobes.load(player.getUniqueId()).put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        layout = Optional.empty();

        assertTrue(source.items(player).isEmpty());
    }

    @Test
    void anEmptyWardrobeCountsNothing() {
        assertTrue(source.items(player).isEmpty());
    }
}
