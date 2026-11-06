package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.Material;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * One gallery entry. The skin image itself is a file next to gallery.yml, named after the id.
 *
 * @param id   the entry's id
 * @param name the name shown in the gallery menu
 * @param icon the item shown in the gallery menu, or empty for the menu's default icon
 * @param link where the image was downloaded from
 */
public record SkinEntry(SkinId id, String name, Optional<Material> icon, URI link) {
    public SkinEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(link, "link");
    }

    public SkinEntry withName(String newName) {
        return new SkinEntry(id, newName, icon, link);
    }

    public SkinEntry withIcon(Material newIcon) {
        return new SkinEntry(id, name, Optional.of(newIcon), link);
    }

    public SkinEntry withLink(URI newLink) {
        return new SkinEntry(id, name, icon, newLink);
    }
}
