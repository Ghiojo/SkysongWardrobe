package org.skysongdev.skysongwardrobe.npc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.skysongdev.skysongwardrobe.commands.CommandRunner;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcCommandTest {
    @TempDir
    Path folder;
    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock owner;
    private PlayerMock stranger;
    private final FakeNpcSkins skins = new FakeNpcSkins();
    private final FakeDirectory directory = new FakeDirectory();
    private NpcInfo baker;
    private CommandRunner commands;

    /** NPCs by number, plus what each player selected or looks at. */
    private static final class FakeDirectory implements NpcDirectory {
        final Map<Integer, NpcInfo> byNumber = new HashMap<>();
        final Map<UUID, NpcInfo> selected = new HashMap<>();
        final Map<UUID, NpcInfo> lookedAt = new HashMap<>();

        @Override
        public Optional<NpcInfo> byNumber(int number) {
            return Optional.ofNullable(byNumber.get(number));
        }

        @Override
        public Optional<NpcInfo> byId(UUID id) {
            return byNumber.values().stream().filter(npc -> npc.id().equals(id)).findFirst();
        }

        @Override
        public Optional<NpcInfo> selected(CommandSender sender) {
            return Optional.ofNullable(selected.get(((Player) sender).getUniqueId()));
        }

        @Override
        public Optional<NpcInfo> lookedAt(Player player) {
            return Optional.ofNullable(lookedAt.get(player.getUniqueId()));
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        owner = server.addPlayer();
        stranger = server.addPlayer();
        owner.addAttachment(plugin, "skysongwardrobe.npc", true);
        stranger.addAttachment(plugin, "skysongwardrobe.npc", true);
        baker = new NpcInfo(UUID.randomUUID(), 7, "Baker", Optional.of(owner.getUniqueId()));
        skins.dressable.add(baker.id());
        directory.byNumber.put(7, baker);
        WardrobeRepository wardrobes = new WardrobeRepository(folder, Logger.getAnonymousLogger());
        NpcAccess access = NpcAccess.standard(() -> plugin.configuration().settings().npcPermissions());
        WardrobeKinds kinds = new WardrobeKinds().register(WardrobeKind.NPC, wardrobes, (npc, editor) -> { });
        NpcOpener opener = new NpcOpener(skins, access,
                new WardrobeOpener(kinds, FakeItemTags::new, plugin.configuration(), server));
        commands = new CommandRunner(new NpcCommand(directory, opener, FakeItemTags::new, plugin.configuration(),
                new NpcArchive(folder.resolve("removed"), Logger.getAnonymousLogger()), server));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void anOwnerOpensTheirNpcByNumber() throws CommandSyntaxException {
        commands.run(owner, "npc 7");

        assertTrue(looksAtBaker(owner));
    }

    @Test
    void theSelectedNpcComesBeforeTheOneLookedAt() throws CommandSyntaxException {
        NpcInfo guard = new NpcInfo(UUID.randomUUID(), 8, "Guard", Optional.of(owner.getUniqueId()));
        skins.dressable.add(guard.id());
        directory.selected.put(owner.getUniqueId(), baker);
        directory.lookedAt.put(owner.getUniqueId(), guard);

        commands.run(owner, "npc");

        assertTrue(looksAtBaker(owner));
    }

    @Test
    void withoutASelectionTheNpcLookedAtOpens() throws CommandSyntaxException {
        directory.lookedAt.put(owner.getUniqueId(), baker);

        commands.run(owner, "npc");

        assertTrue(looksAtBaker(owner));
    }

    @Test
    void noNpcIsExplained() throws CommandSyntaxException {
        commands.run(owner, "npc");
        commands.run(owner, "npc 99");

        assertEquals("[Wardrobe] Select an NPC with /npc sel, look at one, or give its number.", actionBar(owner));
        assertEquals("[Wardrobe] Select an NPC with /npc sel, look at one, or give its number.", actionBar(owner));
    }

    @Test
    void someoneElsesNpcIsRefused() throws CommandSyntaxException {
        commands.run(stranger, "npc 7");

        assertFalse(looksAtBaker(stranger));
        assertEquals("[Wardrobe] This NPC isn't yours.", actionBar(stranger));
    }

    @Test
    void aNonPlayerNpcIsRefused() throws CommandSyntaxException {
        skins.dressable.remove(baker.id());

        commands.run(owner, "npc 7");

        assertEquals("[Wardrobe] Only player NPCs can wear a wardrobe.", actionBar(owner));
    }

    @Test
    void anNpcSomeoneIsDressingIsRefused() throws CommandSyntaxException {
        PlayerMock staff = server.addPlayer();
        staff.addAttachment(plugin, "skysongwardrobe.npc.other", true);
        commands.run(staff, "npc 7");

        commands.run(owner, "npc 7");

        assertTrue(looksAtBaker(staff));
        assertEquals("[Wardrobe] Someone else is dressing this NPC right now.", actionBar(owner));
    }

    @Test
    void theConsoleOpensAnNpcForAPlayerWithoutTheAccessRules() throws CommandSyntaxException {
        commands.run(server.getConsoleSender(), "npc 7 " + stranger.getName());

        assertTrue(looksAtBaker(stranger));
    }

    @Test
    void staffOpenAnNpcForSomeoneElse() throws CommandSyntaxException {
        PlayerMock staff = server.addPlayer();
        staff.addAttachment(plugin, "skysongwardrobe.npc.other", true);

        commands.run(staff, "npc 7 " + stranger.getName());

        assertTrue(looksAtBaker(stranger));
        assertFalse(looksAtBaker(staff));
    }

    @Test
    void ownersCantOpenForSomeoneElse() {
        assertThrows(CommandSyntaxException.class, () -> commands.run(owner, "npc 7 " + stranger.getName()));
    }

    @Test
    void refusalsGoToWhoeverRanIt() throws CommandSyntaxException {
        commands.run(server.getConsoleSender(), "npc 7 Nobody");
        commands.run(server.getConsoleSender(), "npc 99 " + stranger.getName());
        commands.run(server.getConsoleSender(), "npc 7");

        assertEquals("[Wardrobe] Nobody is not online.", console());
        assertEquals("[Wardrobe] Select an NPC with /npc sel, look at one, or give its number.", console());
        assertEquals("[Wardrobe] From the console, name the player to open the wardrobe for.", console());
        assertFalse(looksAtBaker(stranger));
    }

    private String console() {
        return PlainTextComponentSerializer.plainText().serialize(server.getConsoleSender().nextComponentMessage());
    }

    @Test
    void playersWithoutAnNpcPermissionDontSeeTheCommand() {
        PlayerMock nobody = server.addPlayer();

        assertThrows(CommandSyntaxException.class, () -> commands.run(nobody, "npc 7"));
    }

    @Test
    void staffOpenTheRemovedNpcsMenu() throws CommandSyntaxException, java.io.IOException {
        PlayerMock staff = server.addPlayer();
        staff.addAttachment(plugin, "skysongwardrobe.npc.other", true);
        commands.run(staff, "npc removed");
        assertEquals("[Wardrobe] No removed NPC has items left in its wardrobe.", actionBar(staff));

        org.skysongdev.skysongwardrobe.wardrobe.Wardrobe worn = new org.skysongdev.skysongwardrobe.wardrobe.Wardrobe(baker.id());
        worn.put("coat", new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE));
        new NpcArchive(folder.resolve("removed"), Logger.getAnonymousLogger()).store(baker, worn, java.time.Instant.now());
        commands.run(staff, "npc removed");

        assertTrue(staff.getOpenInventory().getTopInventory().getHolder() instanceof RemovedNpcsMenu);
    }

    @Test
    void ownersCantOpenTheRemovedNpcsMenu() {
        assertThrows(CommandSyntaxException.class, () -> commands.run(owner, "npc removed"));
    }

    private boolean looksAtBaker(PlayerMock player) {
        return WardrobeMenu.of(player.getOpenInventory()).filter(menu -> menu.shows(WardrobeKind.NPC, baker.id())).isPresent();
    }

    private static String actionBar(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.nextActionBar());
    }
}
