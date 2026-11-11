package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.skysongdev.skysongwardrobe.api.Menus;
import org.skysongdev.skysongwardrobe.api.Npcs;
import org.skysongdev.skysongwardrobe.api.Outfits;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.mannequin.MannequinOpener;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRepository;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeEditor;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * SkysongWardrobe's implementation of its public API, offered to other plugins through
 * Bukkit's {@link ServicesManager}.
 */
public final class SkysongWardrobeApiImpl implements SkysongWardrobeApi {
    /** The API version this implementation provides. See {@link SkysongWardrobeApi#apiVersion()}. */
    public static final String API_VERSION = "1.0.0";

    private final Menus menus;
    private final NpcsImpl npcs;
    private final Outfits outfits;

    /** The plugin's pieces the API works through. */
    public record Parts(WardrobeOpener opener, MannequinOpener mannequinOpener, WardrobeEditor editor,
                        PluginConfiguration configuration, MannequinRepository mannequins) {
    }

    /**
     * An API on the plugin's openers and editor, guarded to the server's main thread.
     *
     * @param npcs the Citizens hook, empty while Citizens isn't enabled
     */
    public SkysongWardrobeApiImpl(Parts parts, Supplier<Optional<NpcHooks>> npcs, Server server) {
        MainThread mainThread = new MainThread(server::isPrimaryThread);
        this.menus = new MenusImpl(mainThread, parts.opener(), parts.mannequinOpener(), npcs, server);
        this.npcs = new NpcsImpl(mainThread, npcs);
        this.outfits = new OutfitsImpl(mainThread, parts.editor(), parts.configuration(), parts.mannequins(), npcs);
    }

    @Override
    public String apiVersion() {
        return API_VERSION;
    }

    @Override
    public Menus menus() {
        return menus;
    }

    @Override
    public Npcs npcs() {
        return npcs;
    }

    @Override
    public Outfits outfits() {
        return outfits;
    }

    /** Drops the NPC access rules of plugins that disable; register it as a listener. */
    public Listener npcRules() {
        return npcs;
    }

    /** Offers this API to other plugins, owned by {@code owner}. */
    public void register(ServicesManager services, Plugin owner) {
        services.register(SkysongWardrobeApi.class, this, owner, ServicePriority.Normal);
    }

    /** Stops offering this API, so {@link SkysongWardrobeApi#get()} fails until it is registered again. */
    public void unregister(ServicesManager services) {
        services.unregister(SkysongWardrobeApi.class, this);
    }
}
