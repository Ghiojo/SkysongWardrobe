package org.skysongdev.skysongwardrobe.render;

import org.skysongdev.skysongwardrobe.config.RetryDelays;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Counts MineSkin errors in a row and says how long every upload should wait after each one: the first delay, then
 * twice as long each time, up to the longest delay. A success starts the count over.
 */
public final class RetryBackoff {
    /** 2^30 times any sane first delay is already past any sane longest delay. */
    private static final int MAX_DOUBLINGS = 30;

    private final Supplier<RetryDelays> delays;
    private int errors;

    /** @param delays the delays config.yml sets right now, read on every error */
    public RetryBackoff(Supplier<RetryDelays> delays) {
        this.delays = delays;
    }

    /** Counts one more error in a row and returns how long to wait before the next upload. */
    public synchronized Duration failed() {
        RetryDelays current = delays.get();
        Duration wait = current.first().multipliedBy(1L << Math.min(errors, MAX_DOUBLINGS));
        errors++;
        return wait.compareTo(current.max()) > 0 ? current.max() : wait;
    }

    /** The longest wait, for errors where a shorter one would only hit the same wall, such as a refused API key. */
    public Duration longest() {
        return delays.get().max();
    }

    /** Starts the count over, after MineSkin signed a skin again. */
    public synchronized void succeeded() {
        errors = 0;
    }

    /** How many errors came in a row since the last success; 0 when the last answer was a success. */
    public synchronized int errorsInARow() {
        return errors;
    }
}
