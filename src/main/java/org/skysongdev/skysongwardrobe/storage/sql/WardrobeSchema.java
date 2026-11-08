package org.skysongdev.skysongwardrobe.storage.sql;

import java.util.List;

/** Every {@link SchemaStep} SkysongWardrobe's tables went through, oldest first. Only ever appended to. */
public final class WardrobeSchema {
    private WardrobeSchema() {
    }

    public static List<SchemaStep> steps() {
        return List.of(
                new SchemaStep(1, "texture cache", SqlTextureCache::createTables),
                new SchemaStep(2, "wardrobes", SqlWardrobeStore::createTables),
                new SchemaStep(3, "mannequins", SqlMannequinStore::createTables));
    }
}
