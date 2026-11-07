package org.skysongdev.skysongwardrobe.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Suggests the ids in the sender's own gallery. */
final class GalleryIdSuggestions implements SuggestionProvider<CommandSourceStack> {
    private final GalleryRepository galleries;

    GalleryIdSuggestions(GalleryRepository galleries) {
        this.galleries = galleries;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (context.getSource().getSender() instanceof Player player) {
            String prefix = builder.getRemainingLowerCase();
            galleries.existing(player.getUniqueId()).ifPresent(gallery -> gallery.entries().stream()
                    .map(entry -> entry.id().value())
                    .filter(id -> id.startsWith(prefix.toLowerCase(Locale.ROOT)))
                    .forEach(builder::suggest));
        }
        return builder.buildFuture();
    }
}
