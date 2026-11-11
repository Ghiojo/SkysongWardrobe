package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Naming an online player in a command: suggesting names, and finding the one typed. */
public final class OnlinePlayers {
    private OnlinePlayers() {
    }

    /** Suggests the names of online players that start with what was typed. */
    public static SuggestionProvider<CommandSourceStack> suggestions(Server server) {
        return (context, builder) -> {
            server.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    /** The online player called {@code name}; tells {@code sender} when there is none. */
    public static Optional<Player> find(Server server, String name, CommandSender sender,
                                       PluginConfiguration configuration) {
        Optional<Player> online = Optional.ofNullable(server.getPlayerExact(name));
        if (online.isEmpty()) {
            configuration.messages().send(sender, "wardrobe.player-offline", Map.of("player", name));
        }
        return online;
    }

    /** The sender as a player; tells a console sender to name the player instead. */
    public static Optional<Player> self(CommandSender sender, PluginConfiguration configuration) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        configuration.messages().send(sender, "wardrobe.players-only");
        return Optional.empty();
    }
}
