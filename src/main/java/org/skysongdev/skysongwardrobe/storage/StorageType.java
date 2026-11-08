package org.skysongdev.skysongwardrobe.storage;

import java.util.Locale;
import java.util.Optional;

/** Where SkysongWardrobe keeps its data, as {@code storage.type} in config.yml names it. */
public enum StorageType {
    /** Today's files: texture-cache.yml, wardrobes/, mannequins/ and npcs/. */
    YAML,
    /** One local database file, with nothing to set up. */
    SQLITE,
    /** A MySQL or MariaDB server. */
    MYSQL;

    /** The type with this config name, ignoring case and spaces. */
    public static Optional<StorageType> named(String name) {
        for (StorageType type : values()) {
            if (type.name().equals(name.trim().toUpperCase(Locale.ROOT))) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
