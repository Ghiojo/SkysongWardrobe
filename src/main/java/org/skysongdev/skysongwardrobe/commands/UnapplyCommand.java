package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.items.LayerKind;

import java.util.Optional;
import java.util.function.Supplier;

/** {@code /sw unapply <regular|dyed>}: removes that layer tag from the SkysongItems item in the main hand. */
public final class UnapplyCommand implements SubCommand {
    private static final String LAYER = "layer";

    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;

    public UnapplyCommand(Supplier<ItemTagGateway> tags, PluginConfiguration configuration) {
        this.tags = tags;
        this.configuration = configuration;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("unapply")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().applyPermissions()))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(LAYER, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            LayerArgument.NAMES.forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::unapply));
    }

    private int unapply(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        Optional<LayerKind> layer = LayerArgument.parse(StringArgumentType.getString(context, LAYER));
        if (layer.isEmpty()) {
            player.sendMessage(configuration.messages().get("apply.unknown-layer"));
            return 0;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemTagGateway gateway = tags.get();
        if (!gateway.isSkysongItem(held)) {
            player.sendMessage(configuration.messages().get("apply.not-a-skysong-item"));
            return 0;
        }
        if (!gateway.removeLayer(player, held, layer.get())) {
            player.sendMessage(configuration.messages().get("unapply.nothing"));
            return 0;
        }
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(configuration.messages().get("unapply.done"));
        return Command.SINGLE_SUCCESS;
    }
}
