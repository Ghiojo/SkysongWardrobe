package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * The tag in an entity's persistent data that says this plugin placed it as a mannequin. The
 * entity's own UUID then names its file, so the marker holds nothing else.
 */
public final class MannequinMarker {
    private final NamespacedKey key;

    public MannequinMarker(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "mannequin");
    }

    public void mark(Entity entity) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
    }

    public boolean isMarked(Entity entity) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}
