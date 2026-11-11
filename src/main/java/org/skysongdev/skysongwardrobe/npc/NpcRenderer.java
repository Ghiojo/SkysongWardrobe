package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.render.BaseSkin;
import org.skysongdev.skysongwardrobe.render.ItemLayers;
import org.skysongdev.skysongwardrobe.render.OutfitRenderer;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Draws an NPC's outfit onto its base skin through the shared {@link OutfitRenderer}. The NPC's
 * skin and its items' tags are read on the main thread, the base download and render off it.
 * An empty wardrobe puts the base skin back. Whoever closed the menu hears if the render waits
 * for MineSkin, or if the NPC has no skin to draw on.
 */
public final class NpcRenderer {
    private final WardrobeRepository wardrobes;
    private final NpcBaseSkins bases;
    private final NpcSkins skins;
    private final NpcSkinApplier applier;
    private final OutfitRenderer outfits;
    private final PluginConfiguration configuration;
    private final TaskRunner tasks;
    private final Server server;
    private final Logger logger;

    public NpcRenderer(WardrobeRepository wardrobes, NpcBaseSkins bases, NpcSkins skins, NpcSkinApplier applier,
                       OutfitRenderer outfits, PluginConfiguration configuration, TaskRunner tasks, Server server,
                       Logger logger) {
        this.wardrobes = wardrobes;
        this.bases = bases;
        this.skins = skins;
        this.applier = applier;
        this.outfits = outfits;
        this.configuration = configuration;
        this.tasks = tasks;
        this.server = server;
        this.logger = logger;
    }

    /**
     * Renders the NPC's outfit, telling {@code editor} how it went; null when another plugin
     * changed it and nobody is told. Main thread only.
     */
    public void render(UUID npc, UUID editor) {
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty() || !skins.isDressable(npc)) {
            return;
        }
        Optional<SignedTexture> current = skins.current(npc);
        List<ItemLayers> worn = outfits.read(wardrobes.load(npc).worn(layout.get()));
        tasks.async(() -> render(npc, editor, current, worn));
    }

    private void render(UUID npc, UUID editor, Optional<SignedTexture> current, List<ItemLayers> worn) {
        Optional<BaseSkin> base = bases.resolve(npc, current);
        if (base.isEmpty()) {
            tasks.sync(() -> tell(editor, "npc.no-skin"));
            return;
        }
        OutfitRenderer.Outcome outcome = outfits.render(npc, WearerKind.NPC, base.get().image(),
                base.get().armModel(), Optional.of(base.get().texture()), worn);
        switch (outcome) {
            case OutfitRenderer.Outcome.Ready ready -> tasks.sync(() -> applier.apply(npc, ready.texture()));
            case OutfitRenderer.Outcome.Queued queued -> tasks.sync(() -> tell(editor, "npc.queued"));
            case OutfitRenderer.Outcome.NoApiKey none -> {
                if (configuration.settings().verbose()) {
                    logger.info("Not rendering NPC " + npc + ": config.yml has no MineSkin API key.");
                }
            }
        }
    }

    private void tell(UUID editor, String key) {
        Player online = editor == null ? null : server.getPlayer(editor);
        if (online != null) {
            configuration.messages().send(online, key);
        }
    }
}
