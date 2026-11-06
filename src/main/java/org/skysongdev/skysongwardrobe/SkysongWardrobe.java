package org.skysongdev.skysongwardrobe;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongwardrobe.commands.ReloadCommand;
import org.skysongdev.skysongwardrobe.commands.UploadCommand;
import org.skysongdev.skysongwardrobe.commands.WardrobeCommands;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeSettings;
import org.skysongdev.skysongwardrobe.download.SkinDownloader;
import org.skysongdev.skysongwardrobe.download.SkinSource;
import org.skysongdev.skysongwardrobe.gallery.GalleryFolderListener;
import org.skysongdev.skysongwardrobe.gallery.GalleryFolders;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.MojangNameLookup;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;
import org.skysongdev.skysongwardrobe.items.TagRegistrationListener;
import org.skysongdev.skysongwardrobe.prompt.DialogConfirmationPrompt;
import org.skysongdev.skysongwardrobe.scheduling.BukkitTaskRunner;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

/**
 * Entry point of SkysongWardrobe. It only builds and wires the plugin's parts;
 * each feature lives in its own package and is registered here as it is added.
 *
 * <p>Not final, because MockBukkit loads the plugin in tests through a generated subclass.
 */
public class SkysongWardrobe extends JavaPlugin {
    private static final Duration NAME_LOOKUP_TIMEOUT = Duration.ofSeconds(5);

    private PluginConfiguration configuration;
    private GalleryFolders galleryFolders;
    private GalleryRepository galleries;

    @Override
    public void onEnable() {
        configuration = new PluginConfiguration(this);
        configuration.reload();
        if (!configuration.settings().hasMineSkinApiKey()) {
            getLogger().warning("No MineSkin API key in config.yml, so skins will not render.");
        }
        getServer().getPluginManager().registerEvents(new TagRegistrationListener(this, SkysongItemsApi::get), this);

        galleryFolders = new GalleryFolders(getDataFolder().toPath().resolve("skins"),
                new MojangNameLookup(NAME_LOOKUP_TIMEOUT), getLogger());
        try {
            galleryFolders.scan();
        } catch (IOException failed) {
            getLogger().log(Level.SEVERE, "Could not read the skins folder.", failed);
        }
        getServer().getPluginManager().registerEvents(new GalleryFolderListener(this, galleryFolders), this);
        galleries = new GalleryRepository(galleryFolders, getLogger());

        SkinUploader uploader = new SkinUploader(galleries, () -> downloader(configuration.settings()),
                new DialogConfirmationPrompt(), configuration, new BukkitTaskRunner(this), Clock.systemUTC(), getLogger());

        WardrobeCommands commands = new WardrobeCommands(List.of(
                new ReloadCommand(configuration),
                new UploadCommand(uploader, configuration)));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> commands.register(event.registrar()));
    }

    /** Built per upload, so a reload's new size limit and timeout apply at once. */
    private static SkinSource downloader(WardrobeSettings settings) {
        return new SkinDownloader(Duration.ofSeconds(settings.downloadTimeoutSeconds()), settings.maxDownloadKb() * 1024);
    }

    @Override
    public void onDisable() {
        // Nothing to release yet.
    }

    /** The loaded config.yml and messages.yml. */
    public PluginConfiguration configuration() {
        return configuration;
    }

    /** Where each player's gallery folder is. */
    public GalleryFolders galleryFolders() {
        return galleryFolders;
    }

    /** One loaded gallery per player. */
    public GalleryRepository galleries() {
        return galleries;
    }
}
