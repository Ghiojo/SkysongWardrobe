package org.skysongdev.skysongwardrobe.render;

import org.skysongdev.skysongwardrobe.config.RetryDelays;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one queue every skin upload goes through. It keeps at most one job per wearer (a newer
 * outfit replaces the waiting one), uploads one job at a time, always takes players before
 * mannequins, and waits the per-wearer cooldown between a wearer's uploads. It pauses every upload, keeping the
 * job, for as long as MineSkin asks after a rate limit, for a growing {@link RetryBackoff} wait while MineSkin is
 * unavailable, and for the longest wait (or until {@link #resume()}) after the API key is turned down. So a job
 * is retried until it is signed or a newer outfit replaces it. Waiting jobs are saved to render-queue.yml, so a
 * restart or a long outage never drops one. Call {@link #runNext()} from a repeating async task.
 */
public final class RenderQueue {
    private final RenderQueueFile file;
    private final SkinSigner signer;
    private final TextureCache cache;
    private final Supplier<Duration> cooldown;
    private final Clock clock;
    private final RenderListener listener;
    private final RetryBackoff backoff;
    private final BooleanSupplier verbose;
    private final Logger logger;
    private final Map<UUID, RenderJob> waiting = new LinkedHashMap<>();
    private final Map<UUID, Instant> lastUpload = new HashMap<>();
    private final Map<UUID, Integer> failedAttempts = new HashMap<>();
    private final ReentrantLock running = new ReentrantLock();
    private Instant pausedUntil = Instant.MIN;
    private boolean pausedForKey;

    private RenderQueue(RenderQueueFile file, SkinSigner signer, TextureCache cache, Supplier<Duration> cooldown,
                        Clock clock, RenderListener listener, RetryBackoff backoff, BooleanSupplier verbose,
                        Logger logger) {
        this.file = file;
        this.signer = signer;
        this.cache = cache;
        this.cooldown = cooldown;
        this.clock = clock;
        this.listener = listener;
        this.backoff = backoff;
        this.verbose = verbose;
        this.logger = logger;
    }

    /** A queue holding the jobs saved in {@code path}, with the default retry delays and quiet logging. */
    public static RenderQueue load(Path path, SkinSigner signer, TextureCache cache, Supplier<Duration> cooldown,
                                   Clock clock, RenderListener listener, Logger logger) {
        return load(path, signer, cache, cooldown, clock, listener, new RetryBackoff(() -> RetryDelays.DEFAULT),
                () -> false, logger);
    }

    /**
     * A queue holding the jobs saved in {@code path}.
     *
     * @param backoff how long to pause after MineSkin errors in a row
     * @param verbose whether to log every error, not only the first one in a row
     */
    public static RenderQueue load(Path path, SkinSigner signer, TextureCache cache, Supplier<Duration> cooldown,
                                   Clock clock, RenderListener listener, RetryBackoff backoff, BooleanSupplier verbose,
                                   Logger logger) {
        RenderQueue queue = new RenderQueue(new RenderQueueFile(path, logger), signer, cache, cooldown, clock, listener,
                backoff, verbose, logger);
        queue.file.load().forEach(job -> queue.waiting.put(job.wearer(), job));
        return queue;
    }

    /** Queues a job, replacing the wearer's waiting one if any; the wearer keeps their place in line. */
    public synchronized void submit(RenderJob job) {
        waiting.put(job.wearer(), job);
        failedAttempts.remove(job.wearer());
        save();
    }

    /** Drops the wearer's waiting job, if any, such as for a mannequin that was broken. */
    public synchronized void cancel(UUID wearer) {
        failedAttempts.remove(wearer);
        if (waiting.remove(wearer) != null) {
            save();
        }
    }

    /** Ends a pause for a refused API key at once, such as after {@code /sw reload} read a new one. */
    public synchronized void resume() {
        if (pausedForKey) {
            pausedForKey = false;
            pausedUntil = Instant.MIN;
        }
    }

    /** Whether the wearer has a job waiting. */
    public synchronized boolean isWaiting(UUID wearer) {
        return waiting.containsKey(wearer);
    }

    /** The waiting jobs, oldest first. */
    public synchronized List<RenderJob> waiting() {
        return List.copyOf(waiting.values());
    }

    /**
     * Signs the first job whose wearer is off cooldown, players before mannequins, unless uploads are paused or another
     * call is already signing. Blocks while MineSkin answers.
     */
    public void runNext() {
        if (!running.tryLock()) {
            return;
        }
        try {
            next().ifPresent(this::run);
        } finally {
            running.unlock();
        }
    }

    private synchronized Optional<RenderJob> next() {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            return Optional.empty();
        }
        return waiting.values().stream()
                .filter(job -> offCooldown(job.wearer(), now))
                .min(Comparator.comparing(RenderJob::kind));
    }

    private boolean offCooldown(UUID wearer, Instant now) {
        Instant last = lastUpload.get(wearer);
        return last == null || !now.isBefore(last.plus(cooldown.get()));
    }

    private void run(RenderJob job) {
        Optional<SignedTexture> known = cache.find(job.hash());
        if (known.isPresent()) {
            finish(job);
            listener.signed(job.wearer(), job.kind(), known.get());
            return;
        }
        SignResult result = signer.sign(job.skin(), job.armModel());
        switch (result) {
            case SignResult.Signed signed -> signed(job, signed.texture());
            case SignResult.RateLimited limited -> pause(limited.retryAfter());
            case SignResult.Unavailable unavailable -> retryLater(job, unavailable.reason(), backoff.failed(), false);
            case SignResult.KeyRejected rejected -> retryLater(job, rejected.reason(), backoff.longest(), true);
            case SignResult.Failed failed -> failed(job, failed.reason());
        }
    }

    private void signed(RenderJob job, SignedTexture texture) {
        backoff.succeeded();
        int failures = forget(job);
        if (failures > 0) {
            logger.info("Signed the skin of " + job.wearer() + " after " + (failures + 1) + " attempts.");
        }
        try {
            cache.put(job.hash(), texture);
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save the texture cache; the skin still applies.", failed);
        }
        uploaded(job);
        listener.signed(job.wearer(), job.kind(), texture);
    }

    private void failed(RenderJob job, String reason) {
        logger.warning("Could not sign the skin of " + job.wearer() + ": " + reason);
        forget(job);
        uploaded(job);
        listener.failed(job.wearer(), job.kind(), reason);
    }

    /**
     * Keeps the job where it is and pauses every upload for {@code wait}. Only the first error in a row is logged,
     * unless verbose is on, so a long MineSkin outage doesn't flood the console.
     */
    private synchronized void retryLater(RenderJob job, String reason, Duration wait, boolean forKey) {
        int failures = waiting.get(job.wearer()) == job ? failedAttempts.merge(job.wearer(), 1, Integer::sum) : 1;
        pausedUntil = clock.instant().plus(wait);
        pausedForKey = forKey;
        if (failures == 1 || verbose.getAsBoolean()) {
            logger.warning("Could not sign the skin of " + job.wearer() + ": " + reason + ". Retrying in "
                    + wait.toSeconds() + "s.");
        }
    }

    /** Forgets the job's failed attempts, returning how many there were. */
    private synchronized int forget(RenderJob job) {
        if (waiting.get(job.wearer()) != job) {
            return 0;
        }
        Integer failures = failedAttempts.remove(job.wearer());
        return failures == null ? 0 : failures;
    }

    private synchronized void pause(Duration retryAfter) {
        pausedUntil = clock.instant().plus(retryAfter);
        pausedForKey = false;
        logger.info("MineSkin rate limit reached; uploads resume in " + retryAfter.toSeconds() + "s.");
    }

    private synchronized void uploaded(RenderJob job) {
        lastUpload.put(job.wearer(), clock.instant());
        finish(job);
    }

    /** Drops the job unless a newer one for the wearer arrived while it was being signed. */
    private synchronized void finish(RenderJob job) {
        if (waiting.get(job.wearer()) == job) {
            waiting.remove(job.wearer());
            save();
        }
    }

    private void save() {
        try {
            file.save(waiting.values());
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save the render queue.", failed);
        }
    }
}
