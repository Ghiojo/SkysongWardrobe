package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Runs {@code /skysongwardrobe ...} against a command tree, as a given sender, without a server's dispatcher. */
public final class CommandRunner {
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    public CommandRunner(SubCommand... subCommands) {
        dispatcher.getRoot().addChild(new WardrobeCommands(List.of(subCommands)).tree());
    }

    /** Runs the command; throws if Brigadier refuses it (unknown, bad syntax or no permission). */
    public int run(CommandSender sender, String arguments) throws CommandSyntaxException {
        return dispatcher.execute(WardrobeCommands.ROOT + " " + arguments, source(sender));
    }

    /** The suggestions for the end of {@code arguments}. */
    public List<String> suggest(CommandSender sender, String arguments) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(WardrobeCommands.ROOT + " " + arguments, source(sender)))
                .join().getList().stream().map(Suggestion::getText).toList();
    }

    private static CommandSourceStack source(CommandSender sender) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        when(source.getExecutor()).thenReturn(sender instanceof Entity entity ? entity : null);
        return source;
    }
}
