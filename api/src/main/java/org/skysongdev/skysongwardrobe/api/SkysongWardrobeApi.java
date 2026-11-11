package org.skysongdev.skysongwardrobe.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point of the SkysongWardrobe API, the supported way for other plugins to open
 * wardrobes and read or change what players, Citizens NPCs and mannequins wear. Classes outside
 * the {@code org.skysongdev.skysongwardrobe.api} package are internal and may change without notice.
 *
 * <p>SkysongWardrobe registers this API with Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * at the end of its {@code onEnable} and removes it in {@code onDisable}. Plugins that use it
 * add {@code depend: [SkysongWardrobe]} (or {@code softdepend}) to their plugin.yml, so they
 * enable after SkysongWardrobe, and get it with {@link #get()}:
 *
 * <pre>{@code
 * SkysongWardrobeApi wardrobe = SkysongWardrobeApi.get();
 * }</pre>
 *
 * <p>or straight from the services manager:
 *
 * <pre>{@code
 * SkysongWardrobeApi wardrobe = Bukkit.getServicesManager().load(SkysongWardrobeApi.class);
 * }</pre>
 *
 * <p>Every method must be called on the server's main thread; they throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>SkysongWardrobe implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface SkysongWardrobeApi {

    /**
     * The API registered by SkysongWardrobe.
     *
     * @return the API
     * @throws IllegalStateException when SkysongWardrobe is not installed or not enabled yet,
     *                               for example because its database is down
     */
    static @NotNull SkysongWardrobeApi get() {
        RegisteredServiceProvider<SkysongWardrobeApi> registration =
                Bukkit.getServicesManager().getRegistration(SkysongWardrobeApi.class);
        if (registration == null) {
            throw new IllegalStateException("The SkysongWardrobe API is not available: SkysongWardrobe is not enabled."
                    + " Add 'depend: [SkysongWardrobe]' to your plugin.yml and check that SkysongWardrobe is installed"
                    + " and started without errors.");
        }
        return registration.getProvider();
    }

    /**
     * The version of the API that the installed SkysongWardrobe implements, as
     * {@code major.minor.patch}. A plugin can compare it with the version it was built
     * against and log a clear message instead of failing with {@link NoSuchMethodError}.
     * Adding methods raises the minor version; removing or changing one raises the major.
     *
     * @return the API version, for example {@code "1.0.0"}
     */
    @NotNull String apiVersion();
}
