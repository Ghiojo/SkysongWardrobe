package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.storage.importing.DatasetImporter;
import org.skysongdev.skysongwardrobe.storage.importing.ImportResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /sw storage import}: copies the YAML files into the database {@code storage.type} names.
 * Console only, since it reads and writes every stored skin at once. Runs off the main thread,
 * one import at a time, and reports one line per kind of data.
 */
public final class StorageImportCommand implements SubCommand {
    private final List<DatasetImporter> importers;
    private final PluginConfiguration configuration;
    private final TaskRunner tasks;
    private final Logger logger;
    private final AtomicBoolean running = new AtomicBoolean();

    public StorageImportCommand(List<DatasetImporter> importers, PluginConfiguration configuration, TaskRunner tasks,
                                Logger logger) {
        this.importers = List.copyOf(importers);
        this.configuration = configuration;
        this.tasks = tasks;
        this.logger = logger;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("storage")
                .requires(source -> source.getSender().hasPermission(Permissions.ADMIN))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("import")
                        .executes(context -> {
                            start(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private void start(CommandSender sender) {
        if (!(sender instanceof ConsoleCommandSender)) {
            send(sender, "storage.console-only", Map.of());
        } else if (importers.isEmpty()) {
            send(sender, "storage.nothing-to-import", Map.of());
        } else if (!running.compareAndSet(false, true)) {
            send(sender, "storage.import-running", Map.of());
        } else {
            send(sender, "storage.import-started", Map.of());
            tasks.async(() -> runAll(sender));
        }
    }

    private void runAll(CommandSender sender) {
        try {
            for (DatasetImporter importer : importers) {
                run(sender, importer);
            }
        } finally {
            running.set(false);
            tasks.sync(() -> send(sender, "storage.import-finished", Map.of()));
        }
    }

    /** One failing dataset is reported and the rest still run. */
    private void run(CommandSender sender, DatasetImporter importer) {
        try {
            ImportResult result = importer.run();
            tasks.sync(() -> send(sender, "storage.import-done", Map.of("dataset", result.dataset(),
                    "imported", String.valueOf(result.imported()), "skipped", String.valueOf(result.skipped()))));
        } catch (Exception failed) {
            logger.log(Level.SEVERE, "Could not import " + importer.dataset() + " into the database.", failed);
            tasks.sync(() -> send(sender, "storage.import-failed", Map.of("dataset", importer.dataset())));
        }
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        configuration.messages().send(sender, key, placeholders);
    }
}
