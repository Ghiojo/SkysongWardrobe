package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;

/** A fixed menu item from wardrobe.yml: a material and a MiniMessage name. */
public record MenuItem(Material material, String name) {
}
