package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

/** Builds {@code /skysongwardrobe}, alias {@code /sw}, out of its subcommands. */
public final class WardrobeCommands {
    static final String ROOT = "skysongwardrobe";
    static final List<String> ALIASES = List.of("sw");
    private static final String DESCRIPTION = "Skin gallery and wardrobe layers";

    private final List<SubCommand> subCommands;

    public WardrobeCommands(List<SubCommand> subCommands) {
        this.subCommands = List.copyOf(subCommands);
    }

    /** The command tree; also used by tests to run commands without a server. */
    public LiteralCommandNode<CommandSourceStack> tree() {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(ROOT);
        subCommands.forEach(subCommand -> root.then(subCommand.node()));
        return root.build();
    }

    /** Registers the tree with Paper; call from the COMMANDS lifecycle event. */
    public void register(Commands registrar) {
        registrar.register(tree(), DESCRIPTION, ALIASES);
    }
}
