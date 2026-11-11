package org.skysongdev.skysongwardrobe.mannequin;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.render.ArmModel;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinInteractListenerTest {
    @TempDir
    Path folder;

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock owner;
    private MannequinMarker marker;
    private Entity entity;
    private MannequinOpener opener;
    private MannequinInteractListener listener;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        WorldMock world = server.addSimpleWorld("world");
        owner = server.addPlayer();
        marker = new MannequinMarker(plugin);
        entity = world.spawn(world.getSpawnLocation(), Zombie.class);
        marker.mark(entity);
        MannequinRepository mannequins = MannequinRepository.load(folder, Logger.getAnonymousLogger());
        mannequins.save(new Mannequin(entity.getUniqueId(), owner.getUniqueId(), new ItemStack(Material.ARMOR_STAND),
                "oak", ArmModel.CLASSIC));
        WardrobeKinds kinds = new WardrobeKinds().register(WardrobeKind.MANNEQUIN,
                new WardrobeRepository(folder.resolve("wardrobes"), Logger.getAnonymousLogger()), (id, editor) -> { });
        opener = new MannequinOpener(mannequins, new OwnerAccess(() -> plugin.configuration().settings().mannequins()),
                new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server));
        listener = new MannequinInteractListener(marker, opener);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerInteractEntityEvent click(PlayerMock player, Entity clicked) {
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, clicked, EquipmentSlot.HAND);
        listener.onInteract(event);
        return event;
    }

    private static boolean looksAtMannequinMenu(PlayerMock player) {
        return WardrobeMenu.of(player.getOpenInventory()).filter(menu -> menu.kind() == WardrobeKind.MANNEQUIN).isPresent();
    }

    private static String message(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.nextActionBar());
    }

    @Test
    void theOwnerOpensItsWardrobe() {
        assertTrue(click(owner, entity).isCancelled());

        assertTrue(looksAtMannequinMenu(owner));
        assertEquals(entity.getUniqueId(), WardrobeMenu.of(owner.getOpenInventory()).orElseThrow().wardrobe().owner());
    }

    @Test
    void someoneElseIsTurnedAway() {
        PlayerMock stranger = server.addPlayer();

        click(stranger, entity);

        assertFalse(looksAtMannequinMenu(stranger));
        assertEquals("[Wardrobe] This mannequin isn't yours.", message(stranger));
    }

    @Test
    void staffMayOpenAnyMannequin() {
        PlayerMock staff = server.addPlayer();
        staff.addAttachment(plugin, "skysongwardrobe.mannequin.other", true);

        click(staff, entity);

        assertTrue(looksAtMannequinMenu(staff));
    }

    @Test
    void onlyOnePersonDressesAMannequinAtATime() {
        PlayerMock staff = server.addPlayer();
        staff.addAttachment(plugin, "skysongwardrobe.mannequin.other", true);
        click(owner, entity);

        click(staff, entity);

        assertFalse(looksAtMannequinMenu(staff));
        assertEquals("[Wardrobe] Someone else is dressing this mannequin right now.", message(staff));
    }

    @Test
    void aCallerThatAlreadyDecidedSkipsTheOwnerCheck() {
        PlayerMock stranger = server.addPlayer();

        assertEquals(OpenOutcome.OPENED, opener.open(stranger, entity, false, OpenSource.API));
        assertTrue(looksAtMannequinMenu(stranger));
    }

    @Test
    void aMarkedEntityWithoutAMannequinOpensNothingQuietly() {
        Entity orphan = entity.getWorld().spawn(entity.getLocation(), Zombie.class);
        marker.mark(orphan);

        assertTrue(click(owner, orphan).isCancelled());
        assertFalse(looksAtMannequinMenu(owner));
        assertEquals(null, owner.nextActionBar());
    }

    @Test
    void otherEntitiesAreLeftAlone() {
        Entity zombie = entity.getWorld().spawn(entity.getLocation(), Zombie.class);

        assertFalse(click(owner, zombie).isCancelled());
        assertFalse(looksAtMannequinMenu(owner));
    }
}
