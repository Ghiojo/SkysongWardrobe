package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.download.SkinDownloader;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.Icons;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /sw edit <id> <name|icon|link> <value...>} on the sender's own gallery. Name and icon
 * change at once; a new link goes through the prompt and download like an upload.
 */
public final class EditCommand implements SubCommand {
    private static final String ID = "id";
    private static final String VALUE = "value";

    private final GalleryRepository galleries;
    private final SkinUploader uploader;
    private final PluginConfiguration configuration;
    private final Logger logger;

    public EditCommand(GalleryRepository galleries, SkinUploader uploader, PluginConfiguration configuration, Logger logger) {
        this.galleries = galleries;
        this.uploader = uploader;
        this.configuration = configuration;
        this.logger = logger;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("edit")
                .requires(source -> source.getSender() instanceof Player player
                        && Permissions.hasAny(player, configuration.settings().galleryPermissions()))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(ID, StringArgumentType.word())
                        .suggests(new GalleryIdSuggestions(galleries))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("name").then(rest(this::rename)))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("icon").then(iconValue()))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("link").then(rest(this::relink))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> rest(Command<CommandSourceStack> action) {
        return RequiredArgumentBuilder.<CommandSourceStack, String>argument(VALUE, StringArgumentType.greedyString())
                .executes(action);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> iconValue() {
        return RequiredArgumentBuilder.<CommandSourceStack, String>argument(VALUE, StringArgumentType.word())
                .suggests((context, builder) -> {
                    Icons.startingWith(builder.getRemaining()).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(this::changeIcon);
    }

    private int rename(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, VALUE).trim();
        return update(context, "edit.name-done", entry -> entry.withName(name));
    }

    private int changeIcon(CommandContext<CommandSourceStack> context) {
        String typed = StringArgumentType.getString(context, VALUE);
        Optional<Material> icon = Icons.parse(typed);
        if (icon.isEmpty()) {
            context.getSource().getSender().sendMessage(configuration.messages().get("edit.invalid-icon", Map.of("icon", typed)));
            return 0;
        }
        return update(context, "edit.icon-done", entry -> entry.withIcon(icon.get()));
    }

    private int relink(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String typed = StringArgumentType.getString(context, VALUE).trim();
        Optional<URI> link = SkinDownloader.httpLink(typed);
        Optional<SkinId> id = id(context);
        if (id.isEmpty()) {
            return 0;
        }
        if (link.isEmpty()) {
            player.sendMessage(configuration.messages().get("upload.invalid-link", Map.of("link", typed)));
            return 0;
        }
        uploader.relink(player, id.get(), link.get());
        return Command.SINGLE_SUCCESS;
    }

    private int update(CommandContext<CommandSourceStack> context, String doneKey, UnaryOperator<SkinEntry> change) {
        Player player = (Player) context.getSource().getSender();
        Optional<SkinId> id = id(context);
        if (id.isEmpty()) {
            return 0;
        }
        Optional<PlayerGallery> gallery = galleries.existing(player.getUniqueId());
        Optional<SkinEntry> entry = gallery.flatMap(g -> g.entry(id.get()));
        if (entry.isEmpty()) {
            player.sendMessage(configuration.messages().get("edit.not-found", Map.of("id", id.get().value())));
            return 0;
        }
        SkinEntry changed = change.apply(entry.get());
        try {
            gallery.get().update(changed);
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not edit " + id.get() + " in the gallery of " + player.getName() + ".", failed);
            player.sendMessage(configuration.messages().get("edit.error", Map.of("id", id.get().value())));
            return 0;
        }
        player.sendMessage(configuration.messages().get(doneKey, Map.of(
                "id", changed.id().value(), "name", changed.name(),
                "icon", changed.icon().map(Material::name).orElse(""))));
        return Command.SINGLE_SUCCESS;
    }

    /** The typed id, or empty after telling the sender it isn't one. */
    private Optional<SkinId> id(CommandContext<CommandSourceStack> context) {
        String typed = StringArgumentType.getString(context, ID);
        Optional<SkinId> id = SkinId.parse(typed);
        if (id.isEmpty()) {
            context.getSource().getSender().sendMessage(configuration.messages().get("edit.not-found", Map.of("id", typed)));
        }
        return id;
    }
}
