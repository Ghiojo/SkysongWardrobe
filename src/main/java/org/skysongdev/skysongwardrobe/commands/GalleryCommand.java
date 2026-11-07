package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;
import org.skysongdev.skysongwardrobe.menu.GalleryMenu;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /sw gallery [player]}: opens a gallery menu. Your own needs a gallery permission;
 * another player's, online or offline, needs that permission's {@code .other}.
 */
public final class GalleryCommand implements SubCommand {
    private static final String PLAYER = "player";

    private final GalleryRepository galleries;
    private final PluginConfiguration configuration;
    private final Server server;

    public GalleryCommand(GalleryRepository galleries, PluginConfiguration configuration, Server server) {
        this.galleries = galleries;
        this.configuration = configuration;
        this.server = server;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("gallery")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().galleryPermissions()))
                .executes(this::openOwn)
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(PLAYER, StringArgumentType.word())
                        .requires(source -> Permissions.hasAnyOther(source.getSender(), configuration.settings().galleryPermissions()))
                        .suggests((context, builder) -> {
                            server.getOnlinePlayers().stream().map(Player::getName)
                                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::openOther));
    }

    private int openOwn(CommandContext<CommandSourceStack> context) {
        Player viewer = (Player) context.getSource().getSender();
        open(viewer, viewer.getUniqueId(), viewer.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int openOther(CommandContext<CommandSourceStack> context) {
        Player viewer = (Player) context.getSource().getSender();
        String typed = StringArgumentType.getString(context, PLAYER);
        Optional<OfflinePlayer> owner = find(typed);
        if (owner.isEmpty()) {
            viewer.sendMessage(configuration.messages().get("gallery.unknown-player", Map.of("player", typed)));
            return 0;
        }
        String name = owner.get().getName() != null ? owner.get().getName() : typed;
        open(viewer, owner.get().getUniqueId(), name);
        return Command.SINGLE_SUCCESS;
    }

    /** An online player by exact name, or one who has played here before; never a web lookup. */
    private Optional<OfflinePlayer> find(String name) {
        Player online = server.getPlayerExact(name);
        return online != null ? Optional.of(online) : Optional.ofNullable(server.getOfflinePlayerIfCached(name));
    }

    private void open(Player viewer, UUID owner, String ownerName) {
        List<SkinEntry> entries = galleries.existing(owner).map(PlayerGallery::entries).orElse(List.of());
        new GalleryMenu(ownerName, entries, configuration.galleryMenu(), configuration.messages()).open(viewer);
    }
}
