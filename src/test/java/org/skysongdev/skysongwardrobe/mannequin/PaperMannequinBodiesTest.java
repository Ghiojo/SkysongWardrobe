package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperMannequinBodiesTest {
    private final UUID id = UUID.randomUUID();
    private final Server server = mock(Server.class);
    private final Mannequin mannequin = mock(Mannequin.class);
    private final EntityEquipment equipment = mock(EntityEquipment.class);

    @BeforeEach
    void setUp() {
        // ItemStack needs a running server.
        MockBukkit.mock();
        when(server.getEntity(id)).thenReturn(mannequin);
        when(mannequin.getEquipment()).thenReturn(equipment);
        when(mannequin.canUseEquipmentSlot(any())).thenAnswer(call ->
                call.getArgument(0) == EquipmentSlot.HEAD || call.getArgument(0) == EquipmentSlot.HAND);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void dressesUsableSlotsWithoutTouchingDropChances() {
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);

        new PaperMannequinBodies(server).dress(id, Map.of(EquipmentSlot.HEAD, helmet));

        verify(equipment).setItem(EquipmentSlot.HEAD, helmet);
        verify(equipment).setItem(EquipmentSlot.HAND, ItemStack.empty());
        verify(equipment, never()).setItem(EquipmentSlot.BODY, ItemStack.empty());
        // Paper throws for drop chances on non-mob entities such as mannequins.
        verify(equipment, never()).setDropChance(any(), anyFloat());
    }
}
