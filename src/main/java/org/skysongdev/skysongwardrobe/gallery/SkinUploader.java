package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.download.DownloadResult;
import org.skysongdev.skysongwardrobe.download.SkinSource;
import org.skysongdev.skysongwardrobe.prompt.ConfirmationPrompt;
import org.skysongdev.skysongwardrobe.prompt.PromptText;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Getting an image into a gallery: a new entry ({@code /sw upload}) or a new link for an
 * existing one ({@code /sw edit <id> link}). Both check first, ask, then (only after Agree)
 * download off the main thread and save. Every outcome is a message from messages.yml.
 */
public final class SkinUploader {
    private final GalleryRepository galleries;
    private final DeletedSkinArchive archive;
    private final Supplier<SkinSource> source;
    private final ConfirmationPrompt prompt;
    private final PluginConfiguration configuration;
    private final TaskRunner tasks;
    private final Clock clock;
    private final Logger logger;

    public SkinUploader(GalleryRepository galleries, DeletedSkinArchive archive, Supplier<SkinSource> source,
                        ConfirmationPrompt prompt, PluginConfiguration configuration, TaskRunner tasks, Clock clock,
                        Logger logger) {
        this.galleries = galleries;
        this.archive = archive;
        this.source = source;
        this.prompt = prompt;
        this.configuration = configuration;
        this.tasks = tasks;
        this.clock = clock;
        this.logger = logger;
    }

    /** Adds a new entry; refused if the player's gallery already has the id. */
    public void start(Player player, UploadRequest request) {
        if (entry(player, request.id()).isPresent()) {
            send(player, "upload.id-taken", request);
            return;
        }
        ask(player, request, png -> add(player, request, png));
    }

    /** Swaps an entry's image for the one at {@code link}, copying the old one to deleted/ first. */
    public void relink(Player player, SkinId id, URI link) {
        Optional<SkinEntry> entry = entry(player, id);
        if (entry.isEmpty()) {
            player.sendMessage(configuration.messages().get("edit.not-found", Map.of("id", id.value())));
            return;
        }
        UploadRequest request = new UploadRequest(id, link, entry.get().icon(), entry.get().name());
        ask(player, request, png -> replace(player, request, png));
    }

    private Optional<SkinEntry> entry(Player player, SkinId id) {
        return galleries.existing(player.getUniqueId()).flatMap(gallery -> gallery.entry(id));
    }

    private void ask(Player player, UploadRequest request, Saver saver) {
        PromptText text = PromptText.from(configuration.messages(), "upload.prompt", placeholders(request));
        prompt.ask(player, text, () -> download(player, request, saver), () -> send(player, "upload.declined", request));
    }

    private void download(Player player, UploadRequest request, Saver saver) {
        send(player, "upload.downloading", request);
        SkinSource downloader = source.get();
        tasks.async(() -> {
            String outcome = saveOrExplain(player, request, downloader.download(request.link().toString()), saver);
            tasks.sync(() -> send(player, outcome, request));
        });
    }

    /** Runs off the main thread; returns the key of the message to show. */
    private String saveOrExplain(Player player, UploadRequest request, DownloadResult result, Saver saver) {
        if (result instanceof DownloadResult.Failure failure) {
            return "upload.failed." + failure.reason().name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
        try {
            return saver.save(((DownloadResult.Success) result).png());
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save " + request.id() + " to the gallery of " + player.getName() + ".", failed);
            return "upload.error";
        }
    }

    private String add(Player player, UploadRequest request, byte[] png) throws IOException {
        SkinEntry entry = new SkinEntry(request.id(), request.name(), request.icon(), request.link(), clock.instant());
        PlayerGallery gallery = galleries.claim(player.getUniqueId(), player.getName());
        return gallery.add(entry, png) ? "upload.saved" : "upload.id-taken";
    }

    private String replace(Player player, UploadRequest request, byte[] png) throws IOException {
        PlayerGallery gallery = galleries.claim(player.getUniqueId(), player.getName());
        Optional<SkinEntry> current = gallery.entry(request.id());
        if (current.isEmpty()) {
            return "edit.not-found";
        }
        archive.archive(gallery, request.id());
        gallery.replace(current.get().withLink(request.link(), clock.instant()), png);
        return "edit.link-done";
    }

    private void send(Player player, String key, UploadRequest request) {
        player.sendMessage(configuration.messages().get(key, placeholders(request)));
    }

    private static Map<String, String> placeholders(UploadRequest request) {
        return Map.of("id", request.id().value(), "name", request.name(), "link", request.link().toString());
    }

    /** Saves a downloaded image; returns the key of the message to show. */
    @FunctionalInterface
    private interface Saver {
        String save(byte[] png) throws IOException;
    }
}
