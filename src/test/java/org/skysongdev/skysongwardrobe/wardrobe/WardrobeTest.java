package org.skysongdev.skysongwardrobe.wardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeTest {
    private final Wardrobe wardrobe = new Wardrobe(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void storesOneItemAndHandsOutCopies() {
        ItemStack stack = new ItemStack(Material.LEATHER_CHESTPLATE, 5);
        wardrobe.put("coat", stack);
        stack.editMeta(meta -> meta.displayName(Component.text("changed outside")));

        ItemStack stored = wardrobe.item("coat").orElseThrow();
        assertFalse(stored.getItemMeta().hasDisplayName());
        assertEquals(1, stored.getAmount());
        stored.editMeta(meta -> meta.displayName(Component.text("changed copy")));
        assertFalse(wardrobe.item("coat").orElseThrow().getItemMeta().hasDisplayName());
    }

    @Test
    void emptyItemClearsTheSlot() {
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        wardrobe.put("coat", ItemStack.empty());

        assertEquals(Optional.empty(), wardrobe.item("coat"));
    }

    @Test
    void wornItemsFollowSlotPriorityAndIgnoreUnlistedSlots() {
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        wardrobe.put("boots", new ItemStack(Material.LEATHER_BOOTS));
        wardrobe.put("removed", new ItemStack(Material.DIAMOND));
        WardrobeLayout layout = new WardrobeLayout("t", Material.GLASS_PANE, " ", List.of(
                new SlotDefinition("coat", 10, Set.of("coat"), 30),
                new SlotDefinition("boots", 28, Set.of("boots"), 10),
                new SlotDefinition("gloves", 19, Set.of("gloves"), 20)));

        assertEquals(List.of(Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE),
                wardrobe.worn(layout).stream().map(ItemStack::getType).toList());
        assertTrue(wardrobe.items().containsKey("removed"));
    }
}
