package org.skysongdev.skysongwardrobe.commands;

import org.skysongdev.skysongwardrobe.items.LayerKind;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The {@code <regular|dyed>} argument of /sw apply and /sw unapply. */
final class LayerArgument {
    static final List<String> NAMES = List.of("regular", "dyed");

    private LayerArgument() {
    }

    static Optional<LayerKind> parse(String typed) {
        return switch (typed.toLowerCase(Locale.ROOT)) {
            case "regular" -> Optional.of(LayerKind.REGULAR);
            case "dyed" -> Optional.of(LayerKind.DYABLE);
            default -> Optional.empty();
        };
    }
}
