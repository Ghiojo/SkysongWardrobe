package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.gallery.DeletedSkinArchive;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinId;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** {@code /sw del <id>}: copies the image to deleted/ and removes the entry from the sender's gallery. */
public final class DeleteCommand implements SubCommand {
    private static final String ID = "id";

    private final GalleryRepository galleries;
    private final DeletedSkinArchive archive;
    private final PluginConfiguration configuration;
    private final Logger logger;

    public DeleteCommand(GalleryRepository galleries, DeletedSkinArchive archive, PluginConfiguration configuration,
                         Logger logger) {
        this.galleries = galleries;
        this.archive = archive;
        this.configuration = configuration;
        this.logger = logger;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("del")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().galleryPermissions()))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(ID, StringArgumentType.word())
                        .suggests(new GalleryIdSuggestions(galleries))
                        .executes(this::delete));
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String typed = StringArgumentType.getString(context, ID);
        Optional<SkinId> id = SkinId.parse(typed);
        Optional<PlayerGallery> gallery = galleries.existing(player.getUniqueId());
        if (id.isEmpty() || gallery.isEmpty() || gallery.get().entry(id.get()).isEmpty()) {
            player.sendMessage(configuration.messages().get("delete.not-found", Map.of("id", typed)));
            return 0;
        }
        try {
            archive.delete(gallery.get(), id.get());
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not delete " + id.get() + " from the gallery of " + player.getName() + ".", failed);
            player.sendMessage(configuration.messages().get("delete.error", Map.of("id", typed)));
            return 0;
        }
        player.sendMessage(configuration.messages().get("delete.done", Map.of("id", id.get().value())));
        return Command.SINGLE_SUCCESS;
    }
}
