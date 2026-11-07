package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinEquipmentTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void everySlotSitsInTheReservedColumns() {
        assertTrue(Arrays.stream(MannequinEquipment.values())
                .allMatch(slot -> MannequinEquipment.isReserved(slot.position())));
        assertEquals(Optional.of(MannequinEquipment.HEAD), MannequinEquipment.at(7));
        assertEquals(Optional.of(MannequinEquipment.OFF_HAND), MannequinEquipment.at(26));
        assertEquals(Optional.empty(), MannequinEquipment.at(8));
    }

    @Test
    void onlyTheLastTwoColumnsAreReserved() {
        assertTrue(MannequinEquipment.isReserved(8));
        assertTrue(MannequinEquipment.isReserved(52));
        assertFalse(MannequinEquipment.isReserved(6));
        assertFalse(MannequinEquipment.isReserved(45));
    }

    @Test
    void armourOnlyFitsItsOwnSlot() {
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);

        assertTrue(MannequinEquipment.HEAD.fits(helmet));
        assertFalse(MannequinEquipment.CHEST.fits(helmet));
        assertFalse(MannequinEquipment.FEET.fits(new ItemStack(Material.STICK)));
    }

    @Test
    void handsTakeAnything() {
        assertTrue(MannequinEquipment.MAIN_HAND.fits(new ItemStack(Material.STICK)));
        assertTrue(MannequinEquipment.OFF_HAND.fits(new ItemStack(Material.IRON_HELMET)));
        assertFalse(MannequinEquipment.MAIN_HAND.fits(ItemStack.empty()));
    }

    @Test
    void storesUnderKeysNoLayoutSlotCanUse() {
        assertEquals("equipment:main_hand", MannequinEquipment.MAIN_HAND.wardrobeId());
        assertEquals(EquipmentSlot.LEGS, MannequinEquipment.LEGS.slot());
    }
}
