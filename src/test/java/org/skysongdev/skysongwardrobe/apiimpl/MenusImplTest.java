package org.skysongdev.skysongwardrobe.apiimpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.api.Access;
import org.skysongdev.skysongwardrobe.api.Menus;
import org.skysongdev.skysongwardrobe.api.OpenOptions;
import org.skysongdev.skysongwardrobe.api.OpenResult;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;
import org.skysongdev.skysongwardrobe.commands.WardrobeCommand;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.npc.NpcAccess;
import org.skysongdev.skysongwardrobe.npc.NpcDirectory;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.npc.NpcOpener;
import org.skysongdev.skysongwardrobe.npc.NpcSkins;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenusImplTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock alice;
    private PlayerMock bob;
    private Menus menus;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        // The plugin's own openers need SkysongItems to open a menu, so menus that open use fake item tags.
        menus = new MenusImpl(new MainThread(() -> true), opener(), null, Optional::empty, server);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String actionBar(PlayerMock player) {
        Component text = player.nextActionBar();
        return text == null ? null : PlainTextComponentSerializer.plainText().serialize(text);
    }

    private static boolean shows(Player viewer, WardrobeKind kind, UUID wearer) {
        return WardrobeMenu.of(viewer.getOpenInventory()).filter(menu -> menu.shows(kind, wearer)).isPresent();
    }

    @Test
    void aPlayerWithTheWardrobePermissionOpensTheirOwn() {
        alice.addAttachment(plugin, WardrobeCommand.PERMISSION, true);

        assertEquals(OpenResult.OPENED, menus.openWardrobe(alice, alice.getUniqueId(), OpenOptions.defaults()));
        assertTrue(shows(alice, WardrobeKind.PLAYER, alice.getUniqueId()));
    }

    @Test
    void someoneElsesWardrobeNeedsTheOtherPermissionAndSaysSo() {
        alice.addAttachment(plugin, WardrobeCommand.PERMISSION, true);

        assertEquals(OpenResult.NO_ACCESS, menus.openWardrobe(alice, bob.getUniqueId(), OpenOptions.defaults()));
        assertFalse(WardrobeMenu.of(alice.getOpenInventory()).isPresent());
        assertEquals("[Wardrobe] You can't open that wardrobe.", actionBar(alice));
    }

    @Test
    void bypassOpensAnyWardrobeWithoutPermissions() {
        assertEquals(OpenResult.OPENED,
                menus.openWardrobe(alice, bob.getUniqueId(), OpenOptions.defaults().access(Access.BYPASS)));
        assertTrue(shows(alice, WardrobeKind.PLAYER, bob.getUniqueId()));
    }

    @Test
    void silentRefusalsSendNothing() {
        assertEquals(OpenResult.NO_ACCESS, menus.openWardrobe(alice, bob.getUniqueId(), OpenOptions.defaults().silent()));
        assertNull(actionBar(alice));
    }

    @Test
    void theCloseActionRunsTheTickAfterClosing() {
        List<Player> closed = new ArrayList<>();
        menus.openWardrobe(alice, alice.getUniqueId(), OpenOptions.defaults().access(Access.BYPASS).onClose(closed::add));

        alice.closeInventory();
        assertTrue(closed.isEmpty());
        server.getScheduler().performOneTick();

        assertEquals(List.of(alice), closed);
    }

    @Test
    void npcWardrobesAreUnavailableWithoutCitizens() {
        assertEquals(OpenResult.UNAVAILABLE, SkysongWardrobeApi.get().menus().openNpcWardrobe(alice, 7, OpenOptions.defaults()));
        assertEquals("[Wardrobe] NPC wardrobes need Citizens.", actionBar(alice));
    }

    @Test
    void aMannequinThatIsntLoadedIsNotFound() {
        assertEquals(OpenResult.NOT_FOUND, SkysongWardrobeApi.get().menus().openMannequinWardrobe(alice, UUID.randomUUID(), OpenOptions.defaults()));
        assertEquals("[Wardrobe] That mannequin isn't here any more.", actionBar(alice));
    }

    @Test
    void npcWardrobesOpenByNumberOrIdThroughTheAccessRules() {
        NpcInfo baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.of(bob.getUniqueId()));
        Menus withNpcs = withNpc(baker);

        assertEquals(OpenResult.NO_ACCESS, withNpcs.openNpcWardrobe(alice, 7, OpenOptions.defaults()));
        assertEquals(OpenResult.OPENED, withNpcs.openNpcWardrobe(alice, baker.id(), OpenOptions.defaults().access(Access.BYPASS)));
        assertTrue(shows(alice, WardrobeKind.NPC, baker.id()));
        assertEquals(OpenResult.IN_USE, withNpcs.openNpcWardrobe(bob, 7, OpenOptions.defaults().access(Access.BYPASS)));
        assertEquals(OpenResult.NOT_FOUND, withNpcs.openNpcWardrobe(bob, 8, OpenOptions.defaults()));
    }

    @Test
    void callsOffTheMainThreadAreRefused() {
        MenusImpl offThread = new MenusImpl(new MainThread(() -> false), opener(), null, Optional::empty, server);

        assertThrows(IllegalStateException.class,
                () -> offThread.openWardrobe(alice, alice.getUniqueId(), OpenOptions.defaults()));
    }

    private WardrobeOpener opener() {
        WardrobeKinds kinds = new WardrobeKinds()
                .register(WardrobeKind.PLAYER, plugin.wardrobes(), (owner, editor) -> { })
                .register(WardrobeKind.NPC, new WardrobeRepository(folder, Logger.getAnonymousLogger()), (npc, editor) -> { });
        return new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server);
    }

    private Menus withNpc(NpcInfo npc) {
        NpcDirectory directory = new NpcDirectory() {
            @Override
            public Optional<NpcInfo> byNumber(int number) {
                return number == npc.number() ? Optional.of(npc) : Optional.empty();
            }

            @Override
            public Optional<NpcInfo> byId(UUID id) {
                return id.equals(npc.id()) ? Optional.of(npc) : Optional.empty();
            }

            @Override
            public Optional<NpcInfo> selected(CommandSender sender) {
                return Optional.empty();
            }

            @Override
            public Optional<NpcInfo> lookedAt(Player player) {
                return Optional.empty();
            }
        };
        NpcSkins skins = mock(NpcSkins.class);
        when(skins.isDressable(npc.id())).thenReturn(true);
        NpcOpener npcOpener = new NpcOpener(skins,
                NpcAccess.standard(() -> plugin.configuration().settings().npcPermissions()), opener());
        return new MenusImpl(new MainThread(() -> true), opener(), null,
                () -> Optional.of(new NpcHooks(directory, npcOpener)), server);
    }
}
