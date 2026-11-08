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
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
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
        commands = new CommandRunner(new NpcCommand(directory, skins, access, wardrobes, FakeItemTags::new,
                plugin.configuration(), server));
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
    void playersWithoutAnNpcPermissionDontSeeTheCommand() {
        PlayerMock nobody = server.addPlayer();

        assertThrows(CommandSyntaxException.class, () -> commands.run(nobody, "npc 7"));
    }

    private boolean looksAtBaker(PlayerMock player) {
        return WardrobeMenu.of(player.getOpenInventory()).filter(menu -> menu.shows(WardrobeKind.NPC, baker.id())).isPresent();
    }

    private static String actionBar(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.nextActionBar());
    }
}
