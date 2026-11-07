package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.download.SkinDownloader;
import org.skysongdev.skysongwardrobe.gallery.Icons;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;
import org.skysongdev.skysongwardrobe.gallery.UploadRequest;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /sw upload <id> <link> [icon] [name...]}. The word after the link is the icon if it
 * names an item, and otherwise the start of the name, so {@code /sw upload cape <link> Red Cape}
 * also works. The name defaults to the id.
 */
public final class UploadCommand implements SubCommand {
    private static final String ID = "id";
    private static final String REST = "link [icon] [name]";

    private final SkinUploader uploader;
    private final PluginConfiguration configuration;

    public UploadCommand(SkinUploader uploader, PluginConfiguration configuration) {
        this.uploader = uploader;
        this.configuration = configuration;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("upload")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().galleryPermissions()))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(ID, StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(REST, StringArgumentType.greedyString())
                                .suggests(UploadCommand::suggestIcon)
                                .executes(this::run)));
    }

    private int run(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String typedId = StringArgumentType.getString(context, ID);
        Optional<SkinId> id = SkinId.parse(typedId);
        if (id.isEmpty()) {
            player.sendMessage(configuration.messages().get("upload.invalid-id", Map.of("id", typedId)));
            return 0;
        }
        String[] words = StringArgumentType.getString(context, REST).trim().split("\\s+");
        Optional<URI> link = SkinDownloader.httpLink(words[0]);
        if (link.isEmpty()) {
            player.sendMessage(configuration.messages().get("upload.invalid-link", Map.of("link", words[0])));
            return 0;
        }
        Optional<Material> icon = words.length > 1 ? Icons.parse(words[1]) : Optional.empty();
        int nameStart = icon.isPresent() ? 2 : 1;
        String name = words.length > nameStart
                ? String.join(" ", Arrays.copyOfRange(words, nameStart, words.length))
                : id.get().value();
        uploader.start(player, new UploadRequest(id.get(), link.get(), icon, name));
        return Command.SINGLE_SUCCESS;
    }

    /** Suggests item names while the player is on the word after the link. */
    private static CompletableFuture<Suggestions> suggestIcon(CommandContext<CommandSourceStack> context,
                                                              SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        int space = typed.indexOf(' ');
        if (space < 0 || typed.indexOf(' ', space + 1) >= 0) {
            return builder.buildFuture();
        }
        SuggestionsBuilder iconBuilder = builder.createOffset(builder.getStart() + space + 1);
        Icons.startingWith(typed.substring(space + 1)).forEach(iconBuilder::suggest);
        return iconBuilder.buildFuture();
    }
}
