package org.skysongdev.skysongwardrobe.gallery;

import java.util.Optional;
import java.util.UUID;

/**
 * Which gallery entry a layer tag points at, stored on items as {@code <owner uuid>:<id>}.
 * A UUID rather than a name, so the tag keeps working when the owner changes their name.
 */
public record SkinReference(UUID owner, SkinId id) {
    private static final char SEPARATOR = ':';

    /** Reads a stored layer tag value, empty if it isn't one. */
    public static Optional<SkinReference> parse(String stored) {
        if (stored == null) {
            return Optional.empty();
        }
        int separator = stored.indexOf(SEPARATOR);
        if (separator < 0) {
            return Optional.empty();
        }
        Optional<SkinId> id = SkinId.parse(stored.substring(separator + 1));
        try {
            UUID owner = UUID.fromString(stored.substring(0, separator));
            return id.map(skin -> new SkinReference(owner, skin));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }

    /** The value to store in a layer tag. */
    public String stored() {
        return owner.toString() + SEPARATOR + id.value();
    }
}
