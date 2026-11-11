package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.ImageFetcher;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Wires NPC wardrobes into the shared pieces: NPC wardrobes live in {@code npcs/wardrobes/},
 * a closed NPC menu renders through the shared outfit renderer, and signed NPC skins come back
 * through the render queue's router. Knows nothing of Citizens beyond {@link NpcSkins}.
 */
public final class NpcWardrobes {
    private final NpcRepository records;
    private final WardrobeRepository wardrobes;
    private final NpcAccess access;
    private final NpcSkins skins;
    private final PluginConfiguration configuration;
    private final Server server;
    private final NpcLifecycle lifecycle;
    private final NpcArchive archive;

    private NpcWardrobes(NpcRepository records, WardrobeRepository wardrobes, NpcAccess access, NpcSkins skins,
                         PluginConfiguration configuration, Server server, NpcLifecycle lifecycle, NpcArchive archive) {
        this.records = records;
        this.wardrobes = wardrobes;
        this.access = access;
        this.skins = skins;
        this.configuration = configuration;
        this.server = server;
        this.lifecycle = lifecycle;
        this.archive = archive;
    }

    /** Everything the shared pieces need to dress and render NPCs. */
    public record Shared(PluginConfiguration configuration, WardrobeKinds kinds, RenderRouter router, RenderQueue queue,
                         OutfitRenderer outfits, TextureCache cache, ImageFetcher fetcher, TaskRunner tasks) {
    }

    /** Starts NPC wardrobes on {@code skins}, keeping their files under {@code folder}. */
    public static NpcWardrobes start(Plugin plugin, Path folder, NpcSkins skins, Shared shared) {
        return start(plugin, skins, shared, new WardrobeRepository(folder.resolve("wardrobes"), plugin.getLogger()),
                new NpcRepository(folder, plugin.getLogger()),
                new NpcArchive(folder.resolve("removed"), plugin.getLogger()));
    }

    /**
     * Starts NPC wardrobes on {@code skins}, their wardrobes in {@code wardrobes}, their base skins
     * in {@code records} and removed NPCs' wardrobes in {@code archive}.
     */
    public static NpcWardrobes start(Plugin plugin, NpcSkins skins, Shared shared, WardrobeRepository wardrobes,
                                     NpcRepository records, NpcArchive archive) {
        BaseSkins images = new BaseSkins(shared.cache(), shared.fetcher(),
                () -> shared.configuration().settings().verbose(), plugin.getLogger());
        NpcSkinApplier applier = new NpcSkinApplier(skins, shared.tasks());
        NpcRenderer renderer = new NpcRenderer(wardrobes,
                new NpcBaseSkins(records, shared.cache(), images, plugin.getLogger()), skins, applier,
                shared.outfits(), shared.configuration(), shared.tasks(), plugin.getServer(), plugin.getLogger());
        shared.router().route(WearerKind.NPC, applier);
        shared.kinds().register(WardrobeKind.NPC, wardrobes,
                (npc, editor) -> plugin.getServer().getPluginManager().callEvent(new NpcUpdateEvent(npc, editor)));
        plugin.getServer().getPluginManager().registerEvents(new NpcRenderTrigger(renderer), plugin);
        NpcAccess access = NpcAccess.standard(() -> shared.configuration().settings().npcPermissions());
        shared.kinds().register(WardrobeKind.REMOVED_NPC, archive.wardrobes(), (entry, editor) -> discardIfEmpty(archive,
                entry, plugin));
        NpcLifecycle lifecycle = new NpcLifecycle(records, wardrobes, archive, shared.queue(), plugin.getServer(),
                Clock.systemUTC(), plugin.getLogger());
        return new NpcWardrobes(records, wardrobes, access, skins, shared.configuration(), plugin.getServer(), lifecycle,
                archive);
    }

    private static void discardIfEmpty(NpcArchive archive, UUID entry, Plugin plugin) {
        try {
            archive.discardIfEmpty(entry);
        } catch (IOException failed) {
            plugin.getLogger().log(Level.WARNING, "Could not delete the emptied removed-NPC entry " + entry + ".", failed);
        }
    }

    /** Opens NPC wardrobes through {@code opener}, after the NPC checks. */
    public NpcOpener opener(WardrobeOpener opener) {
        return new NpcOpener(skins, access, opener);
    }

    /** {@code /sw npc}, finding NPCs through {@code directory} and opening them through {@code opener}. */
    public NpcCommand command(NpcDirectory directory, NpcOpener opener, Supplier<ItemTagGateway> tags) {
        return new NpcCommand(directory, opener, tags, configuration, archive, server);
    }

    /** Archives the wardrobes of removed NPCs and copies those of copied ones. */
    public NpcLifecycle lifecycle() {
        return lifecycle;
    }

    /** Each dressed NPC's saved base skin. */
    public NpcRepository records() {
        return records;
    }

    /** What each NPC wears, keyed by its Citizens UUID. */
    public WardrobeRepository wardrobes() {
        return wardrobes;
    }

    /** Who may open an NPC's wardrobe; other plugins register their rules here. */
    public NpcAccess access() {
        return access;
    }
}
