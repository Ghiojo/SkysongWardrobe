package org.skysongdev.skysongwardrobe.gallery;

import java.util.Optional;
import java.util.UUID;

/** Finds a player's current name from their UUID. May block, so call it off the main thread. */
@FunctionalInterface
public interface NameLookup {
    /** The current name, or empty if it can't be found right now. */
    Optional<String> currentName(UUID player);
}
