package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.config.MannequinEquipment;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeMenuTest {
    static final int COAT = 10;
    static final int BOOTS = 28;

    private Wardrobe wardrobe;
    private WardrobeMenu menu;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        wardrobe = new Wardrobe(UUID.randomUUID());
        menu = new WardrobeMenu(wardrobe, layout(), tags());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    static WardrobeLayout layout() {
        return new WardrobeLayout("Wardrobe", Material.GRAY_STAINED_GLASS_PANE, " ", List.of(
                new SlotDefinition("coat", COAT, Set.of("coat"), 30),
                new SlotDefinition("boots", BOOTS, Set.of("boots"), 10)));
    }

    static FakeItemTags tags() {
        return new FakeItemTags().type(Material.LEATHER_CHESTPLATE, "coat").type(Material.LEATHER_BOOTS, "boots");
    }

    @Test
    void everyPositionButTheSlotsIsFiller() {
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(0).getType());
        assertNull(menu.getInventory().getItem(COAT));
    }

    @Test
    void placesOneOfAStackIntoAMatchingSlot() {
        WardrobeMenu.Move move = menu.placeInto(COAT, new ItemStack(Material.LEATHER_CHESTPLATE, 3));

        assertEquals(Optional.empty(), move.refusal());
        assertEquals(2, move.rest().getAmount());
        assertEquals(1, menu.getInventory().getItem(COAT).getAmount());
        assertTrue(wardrobe.item("coat").isPresent());
        assertTrue(menu.changed());
    }

    @Test
    void refusesWrongTypesUntaggedItemsAndFullSlots() {
        assertEquals(Optional.of(WardrobeMenu.Refusal.NOT_ACCEPTED),
                menu.placeInto(COAT, new ItemStack(Material.LEATHER_BOOTS)).refusal());
        assertEquals(Optional.of(WardrobeMenu.Refusal.NO_TYPE),
                menu.placeInto(COAT, new ItemStack(Material.DIAMOND)).refusal());
        menu.placeInto(COAT, new ItemStack(Material.LEATHER_CHESTPLATE));
        assertEquals(Optional.of(WardrobeMenu.Refusal.SLOT_FULL),
                menu.placeInto(COAT, new ItemStack(Material.LEATHER_CHESTPLATE)).refusal());
    }

    @Test
    void fillerPositionsTakeNothing() {
        ItemStack coat = new ItemStack(Material.LEATHER_CHESTPLATE);

        assertEquals(coat, menu.placeInto(0, coat).rest());
        assertFalse(menu.changed());
    }

    @Test
    void placeAnywhereFindsTheFreeMatchingSlot() {
        assertEquals(Optional.empty(), menu.placeAnywhere(new ItemStack(Material.LEATHER_BOOTS)).refusal());
        assertTrue(wardrobe.item("boots").isPresent());
        assertEquals(Optional.of(WardrobeMenu.Refusal.NO_FREE_SLOT),
                menu.placeAnywhere(new ItemStack(Material.LEATHER_BOOTS)).refusal());
    }

    @Test
    void takeOutEmptiesTheSlot() {
        menu.placeInto(COAT, new ItemStack(Material.LEATHER_CHESTPLATE));

        assertEquals(Material.LEATHER_CHESTPLATE, menu.takeOut(COAT).orElseThrow().getType());
        assertEquals(Optional.empty(), menu.takeOut(COAT));
        assertEquals(Optional.empty(), menu.takeOut(0));
        assertTrue(wardrobe.item("coat").isEmpty());
    }

    @Test
    void aPlayersMenuShowsFillerWhereEquipmentWouldBe() {
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(MannequinEquipment.HEAD.position()).getType());
        assertEquals(Optional.empty(), menu.placeInto(MannequinEquipment.HEAD.position(), new ItemStack(Material.IRON_HELMET)).refusal());
        assertTrue(wardrobe.items().isEmpty());
    }

    @Test
    void aMannequinWearsArmourInItsLockedSlots() {
        WardrobeMenu mannequin = WardrobeMenu.forMannequin(wardrobe, layout(), tags());
        int head = MannequinEquipment.HEAD.position();

        assertEquals(Optional.of(WardrobeMenu.Refusal.NOT_ACCEPTED),
                mannequin.placeInto(head, new ItemStack(Material.IRON_BOOTS)).refusal());
        assertEquals(Optional.empty(), mannequin.placeInto(head, new ItemStack(Material.IRON_HELMET)).refusal());

        assertEquals(Material.IRON_HELMET, mannequin.getInventory().getItem(head).getType());
        assertTrue(wardrobe.item("equipment:head").isPresent());
        assertEquals(Optional.of(WardrobeMenu.Refusal.SLOT_FULL),
                mannequin.placeInto(head, new ItemStack(Material.GOLDEN_HELMET)).refusal());
        assertEquals(Material.IRON_HELMET, mannequin.takeOut(head).orElseThrow().getType());
        assertTrue(wardrobe.item("equipment:head").isEmpty());
    }

    @Test
    void shiftClickPrefersWardrobeSlotsThenEquipment() {
        WardrobeMenu mannequin = WardrobeMenu.forMannequin(wardrobe, layout(), tags());

        mannequin.placeAnywhere(new ItemStack(Material.LEATHER_CHESTPLATE));
        mannequin.placeAnywhere(new ItemStack(Material.LEATHER_CHESTPLATE));
        mannequin.placeAnywhere(new ItemStack(Material.STICK));

        assertTrue(wardrobe.item("coat").isPresent());
        assertTrue(wardrobe.item("equipment:chest").isPresent());
        assertEquals(Material.STICK, wardrobe.item("equipment:main_hand").orElseThrow().getType());
    }

    @Test
    void equipmentIsNeverWornAsALayer() {
        WardrobeMenu mannequin = WardrobeMenu.forMannequin(wardrobe, layout(), tags());

        mannequin.placeInto(MannequinEquipment.HEAD.position(), new ItemStack(Material.IRON_HELMET));

        assertTrue(wardrobe.worn(layout()).isEmpty());
    }

    @Test
    void anNpcsMenuHasNoEquipmentSlots() {
        WardrobeMenu npc = new WardrobeMenu(WardrobeKind.NPC, wardrobe, layout(), tags());

        assertEquals(Optional.empty(), npc.placeInto(MannequinEquipment.HEAD.position(), new ItemStack(Material.IRON_HELMET)).refusal());
        assertTrue(wardrobe.items().isEmpty());
        assertEquals(Optional.of(WardrobeMenu.Refusal.NO_TYPE), npc.placeAnywhere(new ItemStack(Material.STICK)).refusal());
    }

    @Test
    void aMenuShowsOnlyItsOwnKindAndWearer() {
        WardrobeMenu mannequin = WardrobeMenu.forMannequin(wardrobe, layout(), tags());

        assertEquals(WardrobeKind.MANNEQUIN, mannequin.kind());
        assertTrue(mannequin.shows(WardrobeKind.MANNEQUIN, wardrobe.owner()));
        assertFalse(mannequin.shows(WardrobeKind.NPC, wardrobe.owner()));
        assertFalse(mannequin.shows(WardrobeKind.MANNEQUIN, java.util.UUID.randomUUID()));
        assertEquals(WardrobeKind.PLAYER, menu.kind());
    }

    @Test
    void aRemovedNpcsWardrobeOnlyLetsItemsOut() {
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        WardrobeMenu removed = new WardrobeMenu(WardrobeKind.REMOVED_NPC, wardrobe, layout(), tags());

        assertEquals(Optional.of(WardrobeMenu.Refusal.TAKE_OUT_ONLY),
                removed.placeInto(BOOTS, new ItemStack(Material.LEATHER_BOOTS)).refusal());
        assertEquals(Optional.of(WardrobeMenu.Refusal.TAKE_OUT_ONLY),
                removed.placeAnywhere(new ItemStack(Material.LEATHER_BOOTS)).refusal());
        assertEquals(Material.LEATHER_CHESTPLATE, removed.takeOut(COAT).orElseThrow().getType());
        assertTrue(wardrobe.items().isEmpty());
    }
}
