package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.config.RetryDelays;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryBackoffTest {
    private RetryDelays delays = RetryDelays.DEFAULT;
    private final RetryBackoff backoff = new RetryBackoff(() -> delays);

    @Test
    void doublesTheWaitAfterEachErrorUpToTheLongest() {
        List<Long> waits = Stream.generate(backoff::failed).limit(7).map(Duration::toSeconds).toList();

        assertEquals(List.of(30L, 60L, 120L, 240L, 300L, 300L, 300L), waits);
    }

    @Test
    void neverOverflowsAfterAVeryLongOutage() {
        for (int i = 0; i < 1_000; i++) {
            backoff.failed();
        }

        assertEquals(Duration.ofMinutes(5), backoff.failed());
    }

    @Test
    void aSuccessStartsOver() {
        backoff.failed();
        backoff.failed();

        backoff.succeeded();

        assertEquals(0, backoff.errorsInARow());
        assertEquals(Duration.ofSeconds(30), backoff.failed());
    }

    @Test
    void countsErrorsInARow() {
        backoff.failed();
        backoff.failed();

        assertEquals(2, backoff.errorsInARow());
    }

    @Test
    void usesTheDelaysConfigHasNow() {
        backoff.failed();
        delays = new RetryDelays(Duration.ofSeconds(10), Duration.ofSeconds(15));

        assertEquals(Duration.ofSeconds(15), backoff.failed());
        assertEquals(Duration.ofSeconds(15), backoff.longest());
    }
}
