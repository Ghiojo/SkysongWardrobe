package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;

/** {@code /sw reload}: rereads config.yml, messages.yml and wardrobe.yml. */
public final class ReloadCommand implements SubCommand {
    private final PluginConfiguration configuration;
    private final Runnable afterReload;

    public ReloadCommand(PluginConfiguration configuration) {
        this(configuration, () -> { });
    }

    /** @param afterReload runs once the files are read again, such as to retry uploads a refused API key paused */
    public ReloadCommand(PluginConfiguration configuration, Runnable afterReload) {
        this.configuration = configuration;
        this.afterReload = afterReload;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                .requires(source -> source.getSender().hasPermission(Permissions.ADMIN))
                .executes(context -> {
                    configuration.reload();
                    afterReload.run();
                    String key = configuration.layout().isPresent() ? "reload.done" : "reload.layout-broken";
                    configuration.messages().send(context.getSource().getSender(), key);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
