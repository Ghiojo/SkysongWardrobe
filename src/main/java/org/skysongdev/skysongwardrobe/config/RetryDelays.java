package org.skysongdev.skysongwardrobe.config;

import java.time.Duration;
import java.util.Objects;

/**
 * How long uploads wait after MineSkin errors: {@code first} after the first error in a row, doubled after each
 * further one, never more than {@code max}.
 *
 * @param first the wait after the first error in a row
 * @param max   the longest wait, also used after the API key is turned down
 */
public record RetryDelays(Duration first, Duration max) {
    /** 30 seconds, doubling up to 5 minutes. */
    public static final RetryDelays DEFAULT = new RetryDelays(Duration.ofSeconds(30), Duration.ofMinutes(5));

    public RetryDelays {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(max, "max");
        if (first.isNegative() || first.isZero()) {
            throw new IllegalArgumentException("the first wait must be positive: " + first);
        }
        if (max.compareTo(first) < 0) {
            throw new IllegalArgumentException("the longest wait " + max + " is shorter than the first " + first);
        }
    }
}
