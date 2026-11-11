package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.skysongdev.skysongwardrobe.api.Menus;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;
import org.skysongdev.skysongwardrobe.mannequin.MannequinOpener;
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

    /**
     * An API on the plugin's openers, guarded to the server's main thread.
     *
     * @param npcs the Citizens hook, empty while Citizens isn't enabled
     */
    public SkysongWardrobeApiImpl(WardrobeOpener opener, MannequinOpener mannequins, Supplier<Optional<NpcHooks>> npcs,
                                  Server server) {
        this.menus = new MenusImpl(new MainThread(server::isPrimaryThread), opener, mannequins, npcs, server);
    }

    @Override
    public String apiVersion() {
        return API_VERSION;
    }

    @Override
    public Menus menus() {
        return menus;
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
