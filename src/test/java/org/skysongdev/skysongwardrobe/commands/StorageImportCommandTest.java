package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.scheduling.ImmediateTaskRunner;
import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.storage.importing.ImportResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageImportCommandTest {
    private ServerMock server;
    private SkysongWardrobe plugin;
    private ConsoleCommandSenderMock console;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        console = (ConsoleCommandSenderMock) server.getConsoleSender();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CommandRunner commands(DatasetImporter... importers) {
        return new CommandRunner(new StorageImportCommand(List.of(importers), plugin.configuration(),
                new ImmediateTaskRunner(), Logger.getAnonymousLogger()));
    }

    private static DatasetImporter importer(String dataset, int imported, int skipped) {
        return new DatasetImporter() {
            @Override
            public String dataset() {
                return dataset;
            }

            @Override
            public ImportResult run() {
                return new ImportResult(dataset, imported, skipped);
            }
        };
    }

    private List<String> consoleLines() {
        List<String> lines = new ArrayList<>();
        for (var message = console.nextComponentMessage(); message != null; message = console.nextComponentMessage()) {
            lines.add(PlainTextComponentSerializer.plainText().serialize(message));
        }
        return lines;
    }

    @Test
    void reportsEachDatasetToTheConsole() throws CommandSyntaxException {
        commands(importer("textures", 8412, 3)).run(console, "storage import");

        assertEquals(List.of(
                "[Wardrobe] Importing the YAML files into the database. The files are not changed.",
                "[Wardrobe] textures: 8412 imported, 3 already there.",
                "[Wardrobe] Import finished."), consoleLines());
    }

    @Test
    void aFailingDatasetIsReportedAndTheRestStillRun() throws CommandSyntaxException {
        DatasetImporter broken = new DatasetImporter() {
            @Override
            public String dataset() {
                return "wardrobes";
            }

            @Override
            public ImportResult run() throws IOException {
                throw new IOException("disk on fire");
            }
        };

        commands(broken, importer("textures", 1, 0)).run(console, "storage import");

        assertEquals(List.of(
                "[Wardrobe] Importing the YAML files into the database. The files are not changed.",
                "[Wardrobe] wardrobes: the import failed. The console has the reason; running it again is safe.",
                "[Wardrobe] textures: 1 imported, 0 already there.",
                "[Wardrobe] Import finished."), consoleLines());
    }

    @Test
    void yamlStorageHasNothingToImport() throws CommandSyntaxException {
        commands().run(console, "storage import");

        assertEquals(1, consoleLines().size());
    }

    @Test
    void playersMustUseTheConsoleEvenWithAdmin() throws CommandSyntaxException {
        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, Permissions.ADMIN, true);

        commands(importer("textures", 1, 0)).run(admin, "storage import");

        assertEquals("[Wardrobe] Run /sw storage import from the server console.",
                PlainTextComponentSerializer.plainText().serialize(admin.nextComponentMessage()));
    }

    @Test
    void refusedWithoutAdmin() {
        PlayerMock player = server.addPlayer();

        assertThrows(CommandSyntaxException.class, () -> commands().run(player, "storage import"));
    }
}
