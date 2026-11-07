package org.skysongdev.skysongwardrobe.render;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
 * The one queue every skin upload goes through. It keeps at most one job per player (a newer
 * outfit replaces the waiting one), uploads one job at a time, waits the per-player cooldown
 * between a player's uploads, and pauses every upload for as long as MineSkin asks after a rate
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
        queue.file.load().forEach(job -> queue.waiting.put(job.player(), job));
        return queue;
    }

    /** Queues a job, replacing the player's waiting one if any; the player keeps their place in line. */
    public synchronized void submit(RenderJob job) {
        waiting.put(job.player(), job);
        save();
    }

    /** Whether the player has a job waiting. */
    public synchronized boolean isWaiting(UUID player) {
        return waiting.containsKey(player);
    }

    /** The waiting jobs, oldest first. */
    public synchronized List<RenderJob> waiting() {
        return List.copyOf(waiting.values());
    }

    /**
     * Signs the first job whose player is off cooldown, unless uploads are paused or another
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
        return waiting.values().stream().filter(job -> offCooldown(job.player(), now)).findFirst();
    }

    private boolean offCooldown(UUID player, Instant now) {
        Instant last = lastUpload.get(player);
        return last == null || !now.isBefore(last.plus(cooldown.get()));
    }

    private void run(RenderJob job) {
        Optional<SignedTexture> known = cache.find(job.hash());
        if (known.isPresent()) {
            finish(job);
            listener.signed(job.player(), known.get());
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
        listener.signed(job.player(), texture);
    }

    private void failed(RenderJob job, String reason) {
        logger.warning("Could not sign the skin of " + job.player() + ": " + reason);
        uploaded(job);
        listener.failed(job.player(), reason);
    }

    private synchronized void pause(Duration retryAfter) {
        pausedUntil = clock.instant().plus(retryAfter);
        logger.info("MineSkin rate limit reached; uploads resume in " + retryAfter.toSeconds() + "s.");
    }

    private synchronized void uploaded(RenderJob job) {
        lastUpload.put(job.player(), clock.instant());
        finish(job);
    }

    /** Drops the job unless a newer one for the player arrived while it was being signed. */
    private synchronized void finish(RenderJob job) {
        if (waiting.get(job.player()) == job) {
            waiting.remove(job.player());
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
