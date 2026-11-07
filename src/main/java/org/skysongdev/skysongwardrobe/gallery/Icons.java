package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Gallery icons: any item a player can hold, typed by its material name. */
public final class Icons {
    private Icons() {
    }

    /** The item a typed word names, if it is one a player can hold. */
    public static Optional<Material> parse(String word) {
        Material material = Material.matchMaterial(word);
        return material != null && material.isItem() && !material.isAir() ? Optional.of(material) : Optional.empty();
    }

    /** Lowercased names of every icon starting with {@code prefix}, for tab completion. */
    public static List<String> startingWith(String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(Material.values())
                .filter(material -> !material.isLegacy() && material.isItem() && !material.isAir())
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .filter(name -> name.startsWith(lowered))
                .toList();
    }
}
