package org.skysongdev.skysongwardrobe.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryDelaysTest {
    @Test
    void theFirstWaitMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new RetryDelays(Duration.ZERO, Duration.ofMinutes(5)));
    }

    @Test
    void theLongestWaitCannotBeShorterThanTheFirst() {
        assertThrows(IllegalArgumentException.class, () -> new RetryDelays(Duration.ofMinutes(2), Duration.ofMinutes(1)));
    }

    @Test
    void theLongestMayEqualTheFirst() {
        assertDoesNotThrow(() -> new RetryDelays(Duration.ofMinutes(1), Duration.ofMinutes(1)));
    }
}
