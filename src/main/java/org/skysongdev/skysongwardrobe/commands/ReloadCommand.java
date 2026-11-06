package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;

/** {@code /sw reload}: rereads config.yml, messages.yml and wardrobe.yml. */
public final class ReloadCommand implements SubCommand {
    private final PluginConfiguration configuration;

    public ReloadCommand(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                .requires(source -> source.getSender().hasPermission(Permissions.ADMIN))
                .executes(context -> {
                    configuration.reload();
                    String key = configuration.layout().isPresent() ? "reload.done" : "reload.layout-broken";
                    context.getSource().getSender().sendMessage(configuration.messages().get(key));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
