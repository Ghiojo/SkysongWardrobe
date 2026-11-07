package org.skysongdev.skysongwardrobe.items;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongitems.api.TagType;
import org.skysongdev.skysongitems.api.event.SkysongItemsReadyEvent;
import org.skysongdev.skysongitems.api.event.SkysongItemsRegisterEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registers the wardrobe tags with SkysongItems, and checks that its built-in {@code dye_color}
 * tag is there. SkysongItems forgets registered tags on {@code /si reload} and asks again, so
 * this registers on every {@link SkysongItemsRegisterEvent}, not once.
 */
public final class TagRegistrationListener implements Listener {
    static final List<String> OWN_TAGS = List.of(WardrobeTags.LAYER, WardrobeTags.DYABLE_LAYER, WardrobeTags.TYPE);

    private final Plugin plugin;
    private final Supplier<SkysongItemsApi> api;

    /** @param api looked up only when SkysongItems is ready, since it isn't available earlier */
    public TagRegistrationListener(Plugin plugin, Supplier<SkysongItemsApi> api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler
    public void onRegister(SkysongItemsRegisterEvent event) {
        for (String tag : OWN_TAGS) {
            if (!event.registerTag(plugin, tag, TagType.STRING)) {
                plugin.getLogger().warning("SkysongItems refused the " + tag
                        + " tag, probably because another tag has that id. Wardrobe items cannot use it.");
            }
        }
    }

    @EventHandler
    public void onReady(SkysongItemsReadyEvent event) {
        Optional<TagType> dyeColor = api.get().tags().type(WardrobeTags.DYE_COLOR);
        if (dyeColor.filter(TagType.INT::equals).isEmpty()) {
            plugin.getLogger().warning("SkysongItems has no INT " + WardrobeTags.DYE_COLOR
                    + " tag, so dyable layers will not be tinted.");
        }
    }
}
