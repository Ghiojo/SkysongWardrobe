package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads wardrobe.yml's {@code gallery:} section. Every value has a default, so a bad one falls
 * back with a warning instead of closing the gallery: unlike the wardrobe slots, nothing here
 * can be ambiguous.
 */
public final class GalleryMenuParser {
    static final GalleryMenuLayout DEFAULTS = new GalleryMenuLayout(
            "<dark_gray><player>'s gallery",
            new MenuItem(Material.GRAY_STAINED_GLASS_PANE, " "),
            new MenuItem(Material.ARROW, "<yellow>Previous page"),
            new MenuItem(Material.ARROW, "<yellow>Next page"));

    private final List<String> warnings = new ArrayList<>();

    public GalleryMenuLayout parse(ConfigurationSection wardrobeFile) {
        warnings.clear();
        ConfigurationSection gallery = wardrobeFile.getConfigurationSection("gallery");
        if (gallery == null) {
            gallery = new YamlConfiguration();
        }
        return new GalleryMenuLayout(
                gallery.getString("title", DEFAULTS.title()),
                item(gallery, "filler", DEFAULTS.filler()),
                item(gallery, "previous-page", DEFAULTS.previous()),
                item(gallery, "next-page", DEFAULTS.next()));
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    private MenuItem item(ConfigurationSection gallery, String key, MenuItem fallback) {
        String typed = gallery.getString(key + ".material");
        Material material = fallback.material();
        if (typed != null) {
            Material matched = Material.matchMaterial(typed);
            if (matched == null || !matched.isItem() || matched.isAir()) {
                warnings.add("gallery." + key + ".material " + typed + " is not an item, so " + material + " is used.");
            } else {
                material = matched;
            }
        }
        return new MenuItem(material, gallery.getString(key + ".name", fallback.name()));
    }
}
