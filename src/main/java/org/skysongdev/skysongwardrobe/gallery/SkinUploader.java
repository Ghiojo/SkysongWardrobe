package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.download.DownloadResult;
import org.skysongdev.skysongwardrobe.download.SkinSource;
import org.skysongdev.skysongwardrobe.prompt.ConfirmationPrompt;
import org.skysongdev.skysongwardrobe.prompt.PromptText;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.io.IOException;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The upload flow: check the id is free, ask, then (only after Agree) download off the main
 * thread and save. Every outcome is a message from messages.yml under {@code upload.}.
 */
public final class SkinUploader {
    private final GalleryRepository galleries;
    private final Supplier<SkinSource> source;
    private final ConfirmationPrompt prompt;
    private final PluginConfiguration configuration;
    private final TaskRunner tasks;
    private final Clock clock;
    private final Logger logger;

    public SkinUploader(GalleryRepository galleries, Supplier<SkinSource> source, ConfirmationPrompt prompt,
                        PluginConfiguration configuration, TaskRunner tasks, Clock clock, Logger logger) {
        this.galleries = galleries;
        this.source = source;
        this.prompt = prompt;
        this.configuration = configuration;
        this.tasks = tasks;
        this.clock = clock;
        this.logger = logger;
    }

    public void start(Player player, UploadRequest request) {
        if (taken(player, request.id())) {
            send(player, "upload.id-taken", request);
            return;
        }
        PromptText text = PromptText.from(configuration.messages(), "upload.prompt", placeholders(request));
        prompt.ask(player, text, () -> download(player, request), () -> send(player, "upload.declined", request));
    }

    private boolean taken(Player player, SkinId id) {
        return galleries.existing(player.getUniqueId()).flatMap(gallery -> gallery.entry(id)).isPresent();
    }

    private void download(Player player, UploadRequest request) {
        send(player, "upload.downloading", request);
        SkinSource downloader = source.get();
        tasks.async(() -> {
            String outcome = save(player, request, downloader.download(request.link().toString()));
            tasks.sync(() -> send(player, outcome, request));
        });
    }

    /** Runs off the main thread; returns the key of the message to show. */
    private String save(Player player, UploadRequest request, DownloadResult result) {
        if (result instanceof DownloadResult.Failure failure) {
            return "upload.failed." + failure.reason().name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
        byte[] png = ((DownloadResult.Success) result).png();
        SkinEntry entry = new SkinEntry(request.id(), request.name(), request.icon(), request.link(), clock.instant());
        try {
            PlayerGallery gallery = galleries.claim(player.getUniqueId(), player.getName());
            return gallery.add(entry, png) ? "upload.saved" : "upload.id-taken";
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save " + request.id() + " to the gallery of " + player.getName() + ".", failed);
            return "upload.error";
        }
    }

    private void send(Player player, String key, UploadRequest request) {
        player.sendMessage(configuration.messages().get(key, placeholders(request)));
    }

    private static Map<String, String> placeholders(UploadRequest request) {
        return Map.of("id", request.id().value(), "name", request.name(), "link", request.link().toString());
    }
}
