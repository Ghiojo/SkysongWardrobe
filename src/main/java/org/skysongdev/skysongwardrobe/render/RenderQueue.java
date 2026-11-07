package org.skysongdev.skysongwardrobe.render;

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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one queue every skin upload goes through. It keeps at most one job per wearer (a newer
 * outfit replaces the waiting one), uploads one job at a time, always takes players before
 * mannequins, waits the per-wearer cooldown between a wearer's uploads, and pauses every upload for as long as MineSkin asks after a rate
 * limit. Waiting jobs are saved to render-queue.yml, so a restart or a long rate limit never
 * drops one. Call {@link #runNext()} from a repeating async task.
 */
public final class RenderQueue {
    private final RenderQueueFile file;
    private final SkinSigner signer;
    private final TextureCache cache;
    private final Supplier<Duration> cooldown;
    private final Clock clock;
    private final RenderListener listener;
    private final Logger logger;
    private final Map<UUID, RenderJob> waiting = new LinkedHashMap<>();
    private final Map<UUID, Instant> lastUpload = new HashMap<>();
    private final ReentrantLock running = new ReentrantLock();
    private Instant pausedUntil = Instant.MIN;

    private RenderQueue(RenderQueueFile file, SkinSigner signer, TextureCache cache, Supplier<Duration> cooldown,
                        Clock clock, RenderListener listener, Logger logger) {
        this.file = file;
        this.signer = signer;
        this.cache = cache;
        this.cooldown = cooldown;
        this.clock = clock;
        this.listener = listener;
        this.logger = logger;
    }

    /** A queue holding the jobs saved in {@code path}. */
    public static RenderQueue load(Path path, SkinSigner signer, TextureCache cache, Supplier<Duration> cooldown,
                                   Clock clock, RenderListener listener, Logger logger) {
        RenderQueue queue = new RenderQueue(new RenderQueueFile(path, logger), signer, cache, cooldown, clock, listener, logger);
        queue.file.load().forEach(job -> queue.waiting.put(job.wearer(), job));
        return queue;
    }

    /** Queues a job, replacing the wearer's waiting one if any; the wearer keeps their place in line. */
    public synchronized void submit(RenderJob job) {
        waiting.put(job.wearer(), job);
        save();
    }

    /** Drops the wearer's waiting job, if any, such as for a mannequin that was broken. */
    public synchronized void cancel(UUID wearer) {
        if (waiting.remove(wearer) != null) {
            save();
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
            case SignResult.Failed failed -> failed(job, failed.reason());
        }
    }

    private void signed(RenderJob job, SignedTexture texture) {
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
        uploaded(job);
        listener.failed(job.wearer(), job.kind(), reason);
    }

    private synchronized void pause(Duration retryAfter) {
        pausedUntil = clock.instant().plus(retryAfter);
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
