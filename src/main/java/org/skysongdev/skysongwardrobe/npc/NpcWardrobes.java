package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.ImageFetcher;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKinds;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.nio.file.Path;
import java.util.function.Supplier;

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

    private NpcWardrobes(NpcRepository records, WardrobeRepository wardrobes, NpcAccess access, NpcSkins skins,
                         PluginConfiguration configuration, Server server) {
        this.records = records;
        this.wardrobes = wardrobes;
        this.access = access;
        this.skins = skins;
        this.configuration = configuration;
        this.server = server;
    }

    /** Everything the shared pieces need to dress and render NPCs. */
    public record Shared(PluginConfiguration configuration, WardrobeKinds kinds, RenderRouter router,
                         OutfitRenderer outfits, TextureCache cache, ImageFetcher fetcher, TaskRunner tasks) {
    }

    /** Starts NPC wardrobes on {@code skins}, keeping their files under {@code folder}. */
    public static NpcWardrobes start(Plugin plugin, Path folder, NpcSkins skins, Shared shared) {
        NpcRepository records = new NpcRepository(folder, plugin.getLogger());
        WardrobeRepository wardrobes = new WardrobeRepository(folder.resolve("wardrobes"), plugin.getLogger());
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
        return new NpcWardrobes(records, wardrobes, access, skins, shared.configuration(), plugin.getServer());
    }

    /** {@code /sw npc}, finding NPCs through {@code directory}. */
    public NpcCommand command(NpcDirectory directory, Supplier<ItemTagGateway> tags) {
        return new NpcCommand(directory, skins, access, wardrobes, tags, configuration, server);
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
