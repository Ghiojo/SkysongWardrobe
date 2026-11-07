package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/** One {@code /sw} subcommand, as a Brigadier node with its own arguments and permission check. */
public interface SubCommand {
    LiteralArgumentBuilder<CommandSourceStack> node();
}
