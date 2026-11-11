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
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /wardrobe [player]}: opens your wardrobe with {@code skysongwardrobe.wardrobe}, or
 * another player's, online or offline, with {@code skysongwardrobe.wardrobe.other}.
 */
public final class WardrobeCommand {
    public static final String PERMISSION = "skysongwardrobe.wardrobe";
    static final String ROOT = "wardrobe";
    private static final String PLAYER = "player";
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
                .requires(source -> source.getSender() instanceof Player player && player.hasPermission(PERMISSION))
                .executes(context -> {
                    Player viewer = (Player) context.getSource().getSender();
                    open(viewer, viewer.getUniqueId());
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(PLAYER, StringArgumentType.word())
                        .requires(source -> source.getSender().hasPermission(Permissions.other(PERMISSION)))
                        .suggests((context, builder) -> {
                            server.getOnlinePlayers().stream().map(Player::getName)
                                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::openOther))
                .build();
    }

    public void register(Commands registrar) {
        registrar.register(tree(), DESCRIPTION);
    }

    private int openOther(CommandContext<CommandSourceStack> context) {
        Player viewer = (Player) context.getSource().getSender();
        String typed = StringArgumentType.getString(context, PLAYER);
        Player online = server.getPlayerExact(typed);
        Optional<OfflinePlayer> owner = online != null ? Optional.of(online) : Optional.ofNullable(server.getOfflinePlayerIfCached(typed));
        if (owner.isEmpty()) {
            configuration.messages().send(viewer, "wardrobe.unknown-player", Map.of("player", typed));
            return 0;
        }
        open(viewer, owner.get().getUniqueId());
        return Command.SINGLE_SUCCESS;
    }

    private void open(Player viewer, UUID owner) {
        opener.tell(viewer, WardrobeKind.PLAYER, opener.open(viewer, WardrobeKind.PLAYER, owner));
    }
}
