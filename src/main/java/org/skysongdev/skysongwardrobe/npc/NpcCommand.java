package org.skysongdev.skysongwardrobe.npc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.commands.OnlinePlayers;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.commands.SubCommand;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code /sw npc [number] [player]}: opens a Citizens player NPC's wardrobe. Without a number
 * it opens the NPC the player selected, or else the one they are looking at, so owners who can't use
 * {@code /npc sel} only need to look at theirs. {@link NpcOpener} checks who may open which.
 * {@code /sw npc <number> <player>} opens it for that online player, skipping the access rules
 * because whoever runs it decides: staff, the console, or a menu plugin's console command.
 * {@code /sw npc removed} lists removed NPCs' archived wardrobes, for staff only.
 */
public final class NpcCommand implements SubCommand {
    private static final String NUMBER = "number";
    private static final String PLAYER = "player";

    private final NpcDirectory directory;
    private final NpcOpener opener;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final NpcArchive archive;
    private final Server server;

    public NpcCommand(NpcDirectory directory, NpcOpener opener, Supplier<ItemTagGateway> tags,
                      PluginConfiguration configuration, NpcArchive archive, Server server) {
        this.server = server;
        this.directory = directory;
        this.opener = opener;
        this.tags = tags;
        this.configuration = configuration;
        this.archive = archive;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("npc")
                .requires(source -> source.getSender() instanceof Player player && mayDressNpcs(player)
                        || isStaff(source.getSender()))
                .executes(context -> OnlinePlayers.self(context.getSource().getSender(), configuration)
                        .map(viewer -> open(viewer, directory.selected(viewer).or(() -> directory.lookedAt(viewer))))
                        .orElse(0))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("removed")
                        .requires(source -> source.getSender() instanceof Player && isStaff(source.getSender()))
                        .executes(context -> openRemoved((Player) context.getSource().getSender())))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument(NUMBER, IntegerArgumentType.integer(0))
                        .executes(context -> OnlinePlayers.self(context.getSource().getSender(), configuration)
                                .map(viewer -> open(viewer, directory.byNumber(IntegerArgumentType.getInteger(context, NUMBER))))
                                .orElse(0))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(PLAYER, StringArgumentType.word())
                                .requires(source -> isStaff(source.getSender()))
                                .suggests(OnlinePlayers.suggestions(server))
                                .executes(this::openFor)));
    }

    /** Holding the {@code .other} variant of an NPC permission: every NPC, for anyone. The console holds it. */
    private boolean isStaff(CommandSender sender) {
        return Permissions.hasAnyOther(sender, configuration.settings().npcPermissions());
    }

    /** {@code /sw npc <number> <player>}: the sender already decided, so the access rules are skipped. */
    private int openFor(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Optional<Player> viewer = OnlinePlayers.find(server, StringArgumentType.getString(context, PLAYER), sender,
                configuration);
        if (viewer.isEmpty()) {
            return 0;
        }
        OpenOutcome outcome = opener.open(viewer.get(), directory.byNumber(IntegerArgumentType.getInteger(context, NUMBER)),
                false, OpenSource.COMMAND);
        opener.tell(sender, outcome);
        return outcome == OpenOutcome.OPENED ? Command.SINGLE_SUCCESS : 0;
    }

    private boolean mayDressNpcs(Player player) {
        List<String> permissions = configuration.settings().npcPermissions();
        return Permissions.hasAny(player, permissions) || Permissions.hasAnyOther(player, permissions);
    }

    private int open(Player viewer, Optional<NpcInfo> target) {
        OpenOutcome outcome = opener.open(viewer, target, true, OpenSource.COMMAND);
        opener.tell(viewer, outcome);
        return outcome == OpenOutcome.OPENED ? Command.SINGLE_SUCCESS : 0;
    }

    private int openRemoved(Player viewer) {
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty()) {
            configuration.messages().send(viewer, "wardrobe.closed");
            return 0;
        }
        RemovedNpcsMenu menu = new RemovedNpcsMenu(archive, configuration.galleryMenu(), layout.get(), tags.get(),
                configuration.messages());
        if (menu.isEmpty()) {
            configuration.messages().send(viewer, "npc.removed.none");
            return 0;
        }
        viewer.openInventory(menu.getInventory());
        return Command.SINGLE_SUCCESS;
    }
}
