package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.gallery.SkinReference;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.items.LayerKind;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code /sw apply [player:]id <regular|dyed>}: writes the entry's {@code uuid:id} to a layer
 * tag of the SkysongItems item in the main hand. Players type and see {@code name:id}; the tag
 * stores the owner's UUID, so it survives renames.
 */
public final class ApplyCommand implements SubCommand {
    private static final String ARGUMENTS = "skin_and_layer";
    private static final char OWNER_SEPARATOR = ':';

    private final GalleryRepository galleries;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Server server;

    public ApplyCommand(GalleryRepository galleries, Supplier<ItemTagGateway> tags, PluginConfiguration configuration,
                        Server server) {
        this.galleries = galleries;
        this.tags = tags;
        this.configuration = configuration;
        this.server = server;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("apply")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().applyPermissions()))
                // One greedy argument split by hand: Brigadier's word arguments refuse the ':' in name:id.
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(ARGUMENTS, StringArgumentType.greedyString())
                        .suggests(this::suggest)
                        .executes(this::apply));
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        int space = typed.indexOf(' ');
        if (space < 0) {
            return new GalleryIdSuggestions(galleries).getSuggestions(context, builder);
        }
        SuggestionsBuilder layerBuilder = builder.createOffset(builder.getStart() + space + 1);
        String prefix = typed.substring(space + 1).toLowerCase(Locale.ROOT);
        LayerArgument.NAMES.stream().filter(name -> name.startsWith(prefix)).forEach(layerBuilder::suggest);
        return layerBuilder.buildFuture();
    }

    private int apply(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String[] words = StringArgumentType.getString(context, ARGUMENTS).trim().split("\\s+");
        String typed = words[0];
        Optional<LayerKind> layer = words.length == 2 ? LayerArgument.parse(words[1]) : Optional.empty();
        if (layer.isEmpty()) {
            player.sendMessage(configuration.messages().get("apply.unknown-layer"));
            return 0;
        }
        int separator = typed.indexOf(OWNER_SEPARATOR);
        String ownerName = separator < 0 ? player.getName() : typed.substring(0, separator);
        Optional<OfflinePlayer> owner = separator < 0 ? Optional.of(player) : find(ownerName);
        if (owner.isEmpty()) {
            player.sendMessage(configuration.messages().get("apply.unknown-player", Map.of("player", ownerName)));
            return 0;
        }
        if (!owner.get().getUniqueId().equals(player.getUniqueId())
                && !Permissions.hasAnyOther(player, configuration.settings().applyPermissions())) {
            player.sendMessage(configuration.messages().get("apply.no-permission-other"));
            return 0;
        }
        String idText = typed.substring(separator + 1);
        Optional<SkinId> id = SkinId.parse(idText);
        boolean exists = id.isPresent() && galleries.existing(owner.get().getUniqueId())
                .flatMap(gallery -> gallery.entry(id.get())).isPresent();
        Map<String, String> placeholders = Map.of("player", ownerName, "id", idText.toLowerCase(Locale.ROOT));
        if (!exists) {
            player.sendMessage(configuration.messages().get("apply.not-found", placeholders));
            return 0;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemTagGateway gateway = tags.get();
        if (!gateway.isSkysongItem(held)) {
            player.sendMessage(configuration.messages().get("apply.not-a-skysong-item"));
            return 0;
        }
        gateway.setLayer(player, held, layer.get(), new SkinReference(owner.get().getUniqueId(), id.get()).stored());
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(configuration.messages().get("apply.done", placeholders));
        return Command.SINGLE_SUCCESS;
    }

    private Optional<OfflinePlayer> find(String name) {
        Player online = server.getPlayerExact(name);
        return online != null ? Optional.of(online) : Optional.ofNullable(server.getOfflinePlayerIfCached(name));
    }
}
