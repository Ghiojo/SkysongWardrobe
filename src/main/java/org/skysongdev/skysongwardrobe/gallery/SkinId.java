package org.skysongdev.skysongwardrobe.gallery;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A gallery entry's id: 1 to 32 of {@code a-z}, {@code 0-9} and {@code _}. Typed ids are
 * lowercased first, so {@code Red_Cape} and {@code red_cape} are the same entry. The id is
 * also the entry's file name, which is why nothing else is allowed.
 */
public record SkinId(String value) {
    public static final int MAX_LENGTH = 32;
    private static final Pattern VALID = Pattern.compile("[a-z0-9_]{1," + MAX_LENGTH + "}");

    public SkinId {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a skin id: " + value);
        }
    }

    /** The id a player typed, lowercased, or empty if it has other characters or is too long. */
    public static Optional<SkinId> parse(String typed) {
        if (typed == null) {
            return Optional.empty();
        }
        String lowered = typed.toLowerCase(Locale.ROOT);
        return VALID.matcher(lowered).matches() ? Optional.of(new SkinId(lowered)) : Optional.empty();
    }

    @Override
    public String toString() {
        return value;
    }
}
