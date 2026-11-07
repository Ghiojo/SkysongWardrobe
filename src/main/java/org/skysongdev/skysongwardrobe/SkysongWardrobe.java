package org.skysongdev.skysongwardrobe;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongwardrobe.commands.ApplyCommand;
import org.skysongdev.skysongwardrobe.commands.DeleteCommand;
import org.skysongdev.skysongwardrobe.commands.EditCommand;
import org.skysongdev.skysongwardrobe.commands.GalleryCommand;
import org.skysongdev.skysongwardrobe.commands.ReloadCommand;
import org.skysongdev.skysongwardrobe.commands.UnapplyCommand;
import org.skysongdev.skysongwardrobe.commands.UploadCommand;
import org.skysongdev.skysongwardrobe.commands.WardrobeCommand;
import org.skysongdev.skysongwardrobe.commands.WardrobeCommands;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeSettings;
import org.skysongdev.skysongwardrobe.download.PublicAddressPolicy;
import org.skysongdev.skysongwardrobe.download.SkinDownloader;
import org.skysongdev.skysongwardrobe.download.SkinSource;
import org.skysongdev.skysongwardrobe.gallery.DeletedSkinArchive;
import org.skysongdev.skysongwardrobe.gallery.GalleryFolderListener;
import org.skysongdev.skysongwardrobe.gallery.GalleryFolders;
import org.skysongdev.skysongwardrobe.gallery.GalleryRepository;
import org.skysongdev.skysongwardrobe.gallery.MojangNameLookup;
import org.skysongdev.skysongwardrobe.gallery.SkinUploader;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.items.SkysongItemsTagGateway;
import org.skysongdev.skysongwardrobe.items.TagRegistrationListener;
import org.skysongdev.skysongwardrobe.mannequin.MannequinAccess;
import org.skysongdev.skysongwardrobe.mannequin.MannequinBodies;
import org.skysongdev.skysongwardrobe.mannequin.MannequinBreakListener;
import org.skysongdev.skysongwardrobe.mannequin.MannequinBreaker;
import org.skysongdev.skysongwardrobe.mannequin.MannequinInteractListener;
import org.skysongdev.skysongwardrobe.mannequin.MannequinMarker;
import org.skysongdev.skysongwardrobe.mannequin.MannequinPlaceListener;
import org.skysongdev.skysongwardrobe.mannequin.MannequinPlacer;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRenderTrigger;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRenderer;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRepository;
import org.skysongdev.skysongwardrobe.mannequin.MannequinSkinApplier;
import org.skysongdev.skysongwardrobe.mannequin.OrphanMannequinListener;
import org.skysongdev.skysongwardrobe.mannequin.OwnerAccess;
import org.skysongdev.skysongwardrobe.mannequin.PaperMannequinBodies;
import org.skysongdev.skysongwardrobe.mannequin.PaperMannequinSpawner;
import org.skysongdev.skysongwardrobe.menu.MenuListener;
import org.skysongdev.skysongwardrobe.prompt.DialogConfirmationPrompt;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.ConfiguredSigner;
import org.skysongdev.skysongwardrobe.render.HttpImageFetcher;
import org.skysongdev.skysongwardrobe.render.LayerResolver;
import org.skysongdev.skysongwardrobe.render.MineSkinSigner;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.RenderTrigger;
import org.skysongdev.skysongwardrobe.render.SkinApplier;
import org.skysongdev.skysongwardrobe.render.SkinRenderer;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.BukkitTaskRunner;
import org.skysongdev.skysongwardrobe.stats.SkysongStatsHook;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenuListener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeSessionListener;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Entry point of SkysongWardrobe. It only builds and wires the plugin's parts;
 * each feature lives in its own package and is registered here as it is added.
 *
 * <p>Not final, because MockBukkit loads the plugin in tests through a generated subclass.
 */
public class SkysongWardrobe extends JavaPlugin {
    private static final Duration NAME_LOOKUP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration BASE_SKIN_TIMEOUT = Duration.ofSeconds(10);
    /** How long to wait after a MineSkin rate limit that doesn't say how long. */
    private static final Duration DEFAULT_RATE_LIMIT_WAIT = Duration.ofSeconds(30);
    /** How often the render queue checks for a job it may upload: once a second. */
    private static final long QUEUE_PERIOD_TICKS = 20;
    private static final String SKYSONG_STATS = "SkysongStats";

    private PluginConfiguration configuration;
    private GalleryFolders galleryFolders;
    private GalleryRepository galleries;
    private WardrobeRepository wardrobes;
    private RenderQueue renderQueue;
    private MannequinRepository mannequins;
    private WardrobeRepository mannequinWardrobes;
    private MannequinMarker mannequinMarker;
    private MannequinBodies mannequinBodies;

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

        DeletedSkinArchive archive = new DeletedSkinArchive(getDataFolder().toPath().resolve("deleted"));
        Supplier<ItemTagGateway> itemTags = () -> new SkysongItemsTagGateway(SkysongItemsApi.get());
        SkinUploader uploader = new SkinUploader(galleries, archive, () -> downloader(configuration.settings()),
                new DialogConfirmationPrompt(), configuration, new BukkitTaskRunner(this), Clock.systemUTC(), getLogger());

        WardrobeCommands commands = new WardrobeCommands(List.of(
                new ReloadCommand(configuration),
                new UploadCommand(uploader, configuration),
                new EditCommand(galleries, uploader, configuration, getLogger()),
                new DeleteCommand(galleries, archive, configuration, getLogger()),
                new GalleryCommand(galleries, configuration, getServer()),
                new ApplyCommand(galleries, itemTags, configuration, getServer()),
                new UnapplyCommand(itemTags, configuration)));
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        wardrobes = new WardrobeRepository(getDataFolder().toPath().resolve("wardrobes"), getLogger());
        loadMannequins();
        WardrobeCommand wardrobeCommand = new WardrobeCommand(wardrobes, itemTags, configuration, getServer());
        getServer().getPluginManager().registerEvents(new WardrobeMenuListener(wardrobes, mannequinWardrobes, configuration,
                getServer(), getLogger()), this);
        getServer().getPluginManager().registerEvents(new WardrobeSessionListener(wardrobes, getServer()), this);
        OutfitRenderer outfits = startRendering(itemTags);
        startMannequins(itemTags, outfits);
        hookSkysongStats();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            commands.register(event.registrar());
            wardrobeCommand.register(event.registrar());
        });
    }

    /** Reads every placed mannequin and their wardrobes, and clears out mannequins broken earlier. */
    private void loadMannequins() {
        mannequins = MannequinRepository.load(getDataFolder().toPath().resolve("mannequins"), getLogger());
        mannequinWardrobes = new WardrobeRepository(getDataFolder().toPath().resolve("mannequins").resolve("wardrobes"),
                getLogger());
        mannequinMarker = new MannequinMarker(this);
        getServer().getPluginManager().registerEvents(
                new OrphanMannequinListener(mannequins, mannequinMarker, getLogger()), this);
    }

    /** Placing mannequins, and dressing and rendering them through the shared outfit renderer. */
    private void startMannequins(Supplier<ItemTagGateway> itemTags, OutfitRenderer outfits) {
        MannequinRenderer renderer = new MannequinRenderer(mannequins, mannequinWardrobes, configuration, outfits,
                mannequinBodies, new BukkitTaskRunner(this), getLogger());
        MannequinPlacer placer = new MannequinPlacer(mannequins, mannequinMarker, new PaperMannequinSpawner(), itemTags,
                configuration, mannequin -> renderer.render(mannequin.id()), getLogger());
        getServer().getPluginManager().registerEvents(new MannequinPlaceListener(placer, configuration::messages), this);
        getServer().getPluginManager().registerEvents(new MannequinRenderTrigger(renderer), this);
        MannequinAccess access = new OwnerAccess(() -> configuration.settings().mannequins());
        getServer().getPluginManager().registerEvents(new MannequinInteractListener(mannequins, mannequinWardrobes,
                mannequinMarker, access, itemTags, configuration, getServer()), this);
        MannequinBreaker breaker = new MannequinBreaker(mannequins, mannequinWardrobes, access, renderQueue, getServer(),
                getLogger());
        getServer().getPluginManager().registerEvents(
                new MannequinBreakListener(breaker, mannequinMarker, configuration::messages), this);
    }

    /** SkysongStats is optional; its API classes are only loaded when it is enabled. */
    private void hookSkysongStats() {
        if (!getServer().getPluginManager().isPluginEnabled(SKYSONG_STATS)) {
            return;
        }
        try {
            SkysongStatsHook.register(this, wardrobes, configuration);
            getLogger().info("Worn wardrobe items now count towards SkysongStats.");
        } catch (RuntimeException | LinkageError failed) {
            getLogger().log(Level.WARNING, "Could not hook into SkysongStats; worn items won't count towards stats.", failed);
        }
    }

    /**
     * Wires the skin pipeline: wardrobe changes render, the queue signs through MineSkin, the
     * appliers show the result on players and mannequins. Returns the renderer mannequins share.
     */
    private OutfitRenderer startRendering(Supplier<ItemTagGateway> itemTags) {
        BukkitTaskRunner tasks = new BukkitTaskRunner(this);
        TextureCache cache = TextureCache.load(getDataFolder().toPath().resolve("texture-cache.yml"));
        BaseSkins bases = new BaseSkins(cache, new HttpImageFetcher(BASE_SKIN_TIMEOUT),
                () -> configuration.settings().verbose(), getLogger());
        SkinApplier applier = new SkinApplier(getServer(), tasks);
        mannequinBodies = new PaperMannequinBodies(getServer());
        MannequinSkinApplier mannequinApplier = new MannequinSkinApplier(mannequinBodies, tasks);
        getServer().getPluginManager().registerEvents(mannequinApplier, this);
        ConfiguredSigner signer = new ConfiguredSigner(() -> configuration.settings().mineSkinApiKey(),
                key -> new MineSkinSigner(MineSkinSigner.client(key), DEFAULT_RATE_LIMIT_WAIT));
        renderQueue = RenderQueue.load(getDataFolder().toPath().resolve("render-queue.yml"), signer, cache,
                () -> Duration.ofSeconds(configuration.settings().uploadCooldownSeconds()), Clock.systemUTC(),
                new RenderRouter().route(WearerKind.PLAYER, applier).route(WearerKind.MANNEQUIN, mannequinApplier),
                getLogger());
        OutfitRenderer outfits = new OutfitRenderer(
                () -> new LayerResolver(galleries, itemTags.get(), () -> configuration.settings().verbose(), getLogger()),
                cache, renderQueue, () -> configuration.settings().hasMineSkinApiKey());
        SkinRenderer renderer = new SkinRenderer(wardrobes, configuration, bases, outfits, applier, tasks,
                getServer(), getLogger());
        getServer().getPluginManager().registerEvents(new RenderTrigger(renderer, bases), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, renderQueue::runNext, QUEUE_PERIOD_TICKS, QUEUE_PERIOD_TICKS);
        return outfits;
    }

    /** Built per upload, so a reload's new size limit and timeout apply at once. */
    private static SkinSource downloader(WardrobeSettings settings) {
        return new SkinDownloader(Duration.ofSeconds(settings.downloadTimeoutSeconds()), settings.maxDownloadKb() * 1024,
                PublicAddressPolicy.system());
    }

    @Override
    public void onDisable() {
        // Waiting render jobs are already in render-queue.yml and resume on the next start.
    }

    /** The loaded config.yml and messages.yml. */
    public PluginConfiguration configuration() {
        return configuration;
    }

    /** Where each player's gallery folder is. */
    public GalleryFolders galleryFolders() {
        return galleryFolders;
    }

    /** One loaded wardrobe per player. */
    public WardrobeRepository wardrobes() {
        return wardrobes;
    }

    /** The skins waiting for MineSkin. */
    public RenderQueue renderQueue() {
        return renderQueue;
    }

    /** Every placed mannequin. */
    public MannequinRepository mannequins() {
        return mannequins;
    }

    /** What each mannequin wears, keyed by the mannequin's id. */
    public WardrobeRepository mannequinWardrobes() {
        return mannequinWardrobes;
    }

    /** Tells this plugin's mannequin entities apart. */
    public MannequinMarker mannequinMarker() {
        return mannequinMarker;
    }

    /** One loaded gallery per player. */
    public GalleryRepository galleries() {
        return galleries;
    }
}
