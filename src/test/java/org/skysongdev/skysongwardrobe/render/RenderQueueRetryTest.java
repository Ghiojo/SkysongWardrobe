package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.config.RetryDelays;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How {@link RenderQueue} keeps a job and pauses every upload when MineSkin is unavailable or refuses the key. */
class RenderQueueRetryTest {
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEA = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final SignedTexture SIGNED = new SignedTexture("value", "signature");
    private static final SignResult BUSY = new SignResult.Unavailable("MineSkin refused the skin (HTTP 500): timed out");

    @TempDir
    Path folder;

    private final MutableClock clock = new MutableClock();
    private final Deque<SignResult> answers = new ArrayDeque<>();
    private final List<BufferedImage> uploads = new ArrayList<>();
    private final List<String> heard = new ArrayList<>();
    private final List<String> logged = new ArrayList<>();
    private boolean verbose;
    private RenderQueue queue;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        SkinSigner signer = (skin, armModel) -> {
            uploads.add(skin);
            return answers.removeFirst();
        };
        RenderListener listener = new RenderListener() {
            @Override
            public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
                heard.add("signed " + wearer);
            }

            @Override
            public void failed(UUID wearer, WearerKind kind, String reason) {
                heard.add("failed " + wearer);
            }
        };
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), signer,
                YamlTextureCache.load(folder.resolve("texture-cache.yml")), () -> Duration.ZERO, clock, listener,
                new RetryBackoff(() -> RetryDelays.DEFAULT), () -> verbose, logger);
    }

    @Test
    void anUnavailableMineSkinKeepsTheJobAndPausesEveryUpload() {
        answers.add(BUSY);
        queue.submit(job(ALEX, 1));
        queue.runNext();
        queue.submit(job(BEA, 2));

        clock.advance(Duration.ofSeconds(29));
        queue.runNext();

        assertEquals(1, uploads.size());
        assertTrue(queue.isWaiting(ALEX));
        assertTrue(heard.isEmpty());
    }

    @Test
    void retriesTheJobOnceThePauseEnds() {
        answers.add(BUSY);
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();

        clock.advance(Duration.ofSeconds(30));
        queue.runNext();

        assertEquals(List.of("signed " + ALEX), heard);
        assertFalse(queue.isWaiting(ALEX));
    }

    @Test
    void errorsInARowDoubleThePause() {
        answers.add(BUSY);
        answers.add(BUSY);
        queue.submit(job(ALEX, 1));
        queue.runNext();
        clock.advance(Duration.ofSeconds(30));
        queue.runNext();

        clock.advance(Duration.ofSeconds(59));
        queue.runNext();
        assertEquals(2, uploads.size());

        answers.add(new SignResult.Signed(SIGNED));
        clock.advance(Duration.ofSeconds(1));
        queue.runNext();
        assertEquals(3, uploads.size());
    }

    @Test
    void aSuccessStartsThePausesOver() {
        answers.add(BUSY);
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(BUSY);
        queue.submit(job(ALEX, 1));
        queue.runNext();
        clock.advance(Duration.ofSeconds(30));
        queue.runNext();
        queue.submit(job(BEA, 2));
        queue.runNext();

        answers.add(new SignResult.Signed(SIGNED));
        clock.advance(Duration.ofSeconds(30));
        queue.runNext();

        assertFalse(queue.isWaiting(BEA));
    }

    @Test
    void aNewOutfitReplacesTheJobBeingRetried() {
        answers.add(BUSY);
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        RenderJob newer = job(ALEX, 2);
        queue.submit(newer);

        clock.advance(Duration.ofSeconds(30));
        queue.runNext();

        assertEquals(newer.skin(), uploads.getLast());
        assertFalse(queue.isWaiting(ALEX));
    }

    @Test
    void aRefusedKeyPausesForTheLongestWaitAndKeepsTheJob() {
        answers.add(new SignResult.KeyRejected("MineSkin refused the skin (HTTP 401): bad key"));
        queue.submit(job(ALEX, 1));
        queue.runNext();

        clock.advance(Duration.ofSeconds(299));
        queue.runNext();
        assertEquals(1, uploads.size());

        answers.add(new SignResult.Signed(SIGNED));
        clock.advance(Duration.ofSeconds(1));
        queue.runNext();
        assertEquals(List.of("signed " + ALEX), heard);
    }

    @Test
    void resumeEndsAPauseForARefusedKey() {
        answers.add(new SignResult.KeyRejected("config.yml has no MineSkin API key"));
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();

        queue.resume();
        queue.runNext();

        assertEquals(List.of("signed " + ALEX), heard);
    }

    @Test
    void resumeLeavesOtherPausesAlone() {
        answers.add(BUSY);
        queue.submit(job(ALEX, 1));
        queue.runNext();

        queue.resume();
        queue.runNext();

        assertEquals(1, uploads.size());
    }

    @Test
    void logsOnlyTheFirstErrorAndTheFinalSuccess() {
        answers.add(BUSY);
        answers.add(BUSY);
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        clock.advance(Duration.ofSeconds(30));
        queue.runNext();
        clock.advance(Duration.ofSeconds(60));
        queue.runNext();

        assertEquals(List.of(
                "Could not sign the skin of " + ALEX + ": MineSkin refused the skin (HTTP 500): timed out. Retrying in 30s.",
                "Signed the skin of " + ALEX + " after 3 attempts."), logged);
    }

    @Test
    void verboseLogsEveryError() {
        verbose = true;
        answers.add(BUSY);
        answers.add(BUSY);
        queue.submit(job(ALEX, 1));
        queue.runNext();
        clock.advance(Duration.ofSeconds(30));
        queue.runNext();

        assertEquals(2, logged.size());
        assertTrue(logged.getLast().endsWith("Retrying in 60s."), logged.getLast());
    }

    @Test
    void aSkinMineSkinWillNotTakeIsStillDropped() {
        answers.add(new SignResult.Failed("MineSkin refused the skin (HTTP 400): invalid image"));
        queue.submit(job(ALEX, 1));

        queue.runNext();

        assertEquals(List.of("failed " + ALEX), heard);
        assertFalse(queue.isWaiting(ALEX));
    }

    private static RenderJob job(UUID wearer, int pixel) {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        skin.setRGB(0, 0, 0xFF000000 | pixel);
        return RenderJob.of(wearer, skin, ArmModel.CLASSIC);
    }
}
