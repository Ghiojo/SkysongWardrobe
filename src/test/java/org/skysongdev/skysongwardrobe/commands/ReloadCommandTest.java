package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private CommandRunner commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        commands = new CommandRunner(new ReloadCommand(plugin.configuration()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void adminReloadsTheFiles() throws IOException, CommandSyntaxException {
        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, Permissions.ADMIN, true);
        Files.writeString(plugin.getDataFolder().toPath().resolve("config.yml"), "verbose: true\n");

        commands.run(admin, "reload");

        assertTrue(plugin.configuration().settings().verbose());
        assertEquals("[Wardrobe] Reloaded config.yml, messages.yml and wardrobe.yml.", plain(admin));
    }

    @Test
    void saysSoWhenTheLayoutIsBroken() throws IOException, CommandSyntaxException {
        PlayerMock admin = server.addPlayer();
        admin.setOp(true);
        Path layout = plugin.getDataFolder().toPath().resolve("wardrobe.yml");
        Files.writeString(layout, "slots: {}\n");

        commands.run(admin, "reload");

        assertTrue(plain(admin).contains("wardrobe.yml has errors"));
    }

    @Test
    void refusedWithoutAdmin() {
        PlayerMock player = server.addPlayer();

        // Brigadier treats a node the sender may not use as missing; Paper also leaves it out of tab completion.
        assertThrows(CommandSyntaxException.class, () -> commands.run(player, "reload"));
    }

    private static String plain(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
    }
}
