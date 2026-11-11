package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.skysongdev.skysongwardrobe.api.SkysongWardrobeApi;

/**
 * SkysongWardrobe's implementation of its public API, offered to other plugins through
 * Bukkit's {@link ServicesManager}.
 */
public final class SkysongWardrobeApiImpl implements SkysongWardrobeApi {
    /** The API version this implementation provides. See {@link SkysongWardrobeApi#apiVersion()}. */
    public static final String API_VERSION = "1.0.0";

    @Override
    public String apiVersion() {
        return API_VERSION;
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
