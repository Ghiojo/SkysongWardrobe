package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /wardrobe [player] [viewer]}: opens your wardrobe with {@code skysongwardrobe.wardrobe},
 * or another player's, online or offline, with {@code skysongwardrobe.wardrobe.other}. Naming a
 * viewer opens it for that online player instead, so the console and menu plugins can open
 * wardrobes too.
 */
public final class WardrobeCommand {
    public static final String PERMISSION = "skysongwardrobe.wardrobe";
    static final String ROOT = "wardrobe";
    private static final String PLAYER = "player";
    private static final String VIEWER = "viewer";
    private static final String DESCRIPTION = "Opens a wardrobe";

    private final WardrobeOpener opener;
    private final PluginConfiguration configuration;
    private final Server server;

    public WardrobeCommand(WardrobeOpener opener, PluginConfiguration configuration, Server server) {
        this.opener = opener;
        this.configuration = configuration;
        this.server = server;
    }

    public LiteralCommandNode<CommandSourceStack> tree() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(ROOT)
                .requires(source -> source.getSender() instanceof Player player && player.hasPermission(PERMISSION)
                        || source.getSender().hasPermission(Permissions.other(PERMISSION)))
                .executes(context -> OnlinePlayers.self(context.getSource().getSender(), configuration)
                        .map(viewer -> open(viewer, viewer, viewer.getUniqueId())).orElse(0))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(PLAYER, StringArgumentType.word())
                        .requires(source -> source.getSender().hasPermission(Permissions.other(PERMISSION)))
                        .suggests(OnlinePlayers.suggestions(server))
                        .executes(context -> OnlinePlayers.self(context.getSource().getSender(), configuration)
                                .map(viewer -> openOther(context, viewer)).orElse(0))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(VIEWER, StringArgumentType.word())
                                .suggests(OnlinePlayers.suggestions(server))
                                .executes(context -> OnlinePlayers.find(server, StringArgumentType.getString(context, VIEWER),
                                                context.getSource().getSender(), configuration)
                                        .map(viewer -> openOther(context, viewer)).orElse(0))))
                .build();
    }

    public void register(Commands registrar) {
        registrar.register(tree(), DESCRIPTION);
    }

    /** Opens the wardrobe of the player named in the command for {@code viewer}. */
    private int openOther(CommandContext<CommandSourceStack> context, Player viewer) {
        CommandSender sender = context.getSource().getSender();
        String typed = StringArgumentType.getString(context, PLAYER);
        Player online = server.getPlayerExact(typed);
        Optional<OfflinePlayer> owner = online != null ? Optional.of(online) : Optional.ofNullable(server.getOfflinePlayerIfCached(typed));
        if (owner.isEmpty()) {
            configuration.messages().send(sender, "wardrobe.unknown-player", Map.of("player", typed));
            return 0;
        }
        return open(sender, viewer, owner.get().getUniqueId());
    }

    /** Opens {@code owner}'s wardrobe for {@code viewer}, telling {@code sender} if it did not open. */
    private int open(CommandSender sender, Player viewer, UUID owner) {
        OpenOutcome outcome = opener.open(viewer, WardrobeKind.PLAYER, owner, OpenSource.COMMAND);
        opener.tell(sender, WardrobeKind.PLAYER, outcome);
        return outcome == OpenOutcome.OPENED ? Command.SINGLE_SUCCESS : 0;
    }
}
