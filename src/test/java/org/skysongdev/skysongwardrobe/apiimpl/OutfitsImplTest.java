package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.ChangeResult;
import org.skysongdev.skysongwardrobe.api.Outfits;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;
import org.skysongdev.skysongwardrobe.api.Wearer;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRepository;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeEditor;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutfitsImplTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock alice;
    private WardrobeKinds kinds;
    private final List<UUID> rendered = new ArrayList<>();
    private final List<Player> editors = new ArrayList<>();
    private NpcInfo baker;
    private Outfits outfits;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer();
        kinds = new WardrobeKinds()
                .register(WardrobeKind.PLAYER, repository("players"), this::changed)
                .register(WardrobeKind.NPC, repository("npcs"), this::changed);
        FakeItemTags tags = new FakeItemTags().type(Material.LEATHER_CHESTPLATE, "coat")
                .type(Material.LEATHER_BOOTS, "boots");
        WardrobeEditor editor = new WardrobeEditor(kinds, () -> tags, plugin.configuration(), server,
                Logger.getAnonymousLogger());
        baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.empty());
        WardrobeOpener opener = new WardrobeOpener(kinds, () -> tags, plugin.configuration(), server);
        NpcHooks hooks = NpcFixture.hooks(opener, List::of, baker);
        outfits = new OutfitsImpl(new MainThread(() -> true), editor, plugin.configuration(),
                MannequinRepository.load(folder.resolve("mannequins"), Logger.getAnonymousLogger()),
                () -> Optional.of(hooks));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private WardrobeRepository repository(String name) {
        return new WardrobeRepository(folder.resolve(name), Logger.getAnonymousLogger());
    }

    private void changed(UUID wearer, Player editor) {
        rendered.add(wearer);
        editors.add(editor);
    }

    private static ItemStack coat() {
        return new ItemStack(Material.LEATHER_CHESTPLATE);
    }

    @Test
    void slotsAreThoseOfWardrobeYmlInDrawingOrder() {
        assertEquals(List.of("boots", "gloves", "coat"), outfits.slots());
    }

    @Test
    void anItemGoesIntoAMatchingSlotSavesAndRendersAgain() {
        Wearer wearer = Wearer.player(alice.getUniqueId());

        assertEquals(ChangeResult.CHANGED, outfits.setItem(wearer, "coat", coat().asQuantity(3)));

        assertEquals(coat(), outfits.item(wearer, "coat").orElseThrow());
        assertEquals(Map.of("coat", coat()), outfits.outfit(wearer));
        assertEquals(List.of(alice.getUniqueId()), rendered);
        assertEquals(coat(), kinds.get(WardrobeKind.PLAYER).orElseThrow().wardrobes().load(alice.getUniqueId())
                .item("coat").orElseThrow());
    }

    @Test
    void theMenusSlotRulesApply() {
        Wearer wearer = Wearer.player(alice.getUniqueId());

        assertEquals(ChangeResult.WRONG_SLOT, outfits.setItem(wearer, "boots", coat()));
        assertEquals(ChangeResult.NOT_WEARABLE, outfits.setItem(wearer, "coat", new ItemStack(Material.DIAMOND)));
        assertEquals(ChangeResult.UNKNOWN_SLOT, outfits.setItem(wearer, "hat", coat()));
        assertEquals(Map.of(), outfits.outfit(wearer));
        assertTrue(rendered.isEmpty());
    }

    @Test
    void clearingEmptiesTheSlot() {
        Wearer wearer = Wearer.player(alice.getUniqueId());
        outfits.setItem(wearer, "coat", coat());

        assertEquals(ChangeResult.CHANGED, outfits.clear(wearer, "coat"));

        assertEquals(Optional.empty(), outfits.item(wearer, "coat"));
    }

    @Test
    void anOpenMenuShowsTheChangeAtOnce() {
        new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server)
                .open(alice, WardrobeKind.PLAYER, alice.getUniqueId(), OpenSource.API);

        outfits.setItem(Wearer.player(alice.getUniqueId()), "coat", coat());

        assertEquals(Material.LEATHER_CHESTPLATE, alice.getOpenInventory().getTopInventory().getItem(10).getType());
    }

    @Test
    void npcsAreDressedWithNobodyToTell() {
        assertEquals(ChangeResult.CHANGED, outfits.setItem(Wearer.npc(baker.id()), "coat", coat()));

        assertEquals(List.of(baker.id()), rendered);
        assertEquals(java.util.Collections.singletonList(null), editors);
    }

    @Test
    void unknownWearersAreNotFound() {
        assertEquals(ChangeResult.NOT_FOUND, outfits.setItem(Wearer.npc(UUID.randomUUID()), "coat", coat()));
        assertEquals(ChangeResult.NOT_FOUND, outfits.setItem(Wearer.mannequin(UUID.randomUUID()), "coat", coat()));
        assertEquals(Map.of(), outfits.outfit(Wearer.mannequin(UUID.randomUUID())));
    }

    @Test
    void npcOutfitsAreUnavailableWithoutCitizens() {
        Outfits real = SkysongWardrobeApi.get().outfits();

        assertEquals(ChangeResult.UNAVAILABLE, real.setItem(Wearer.npc(baker.id()), "coat", coat()));
        assertEquals(Map.of(), real.outfit(Wearer.npc(baker.id())));
    }
}
