package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderQueueTest {
    private static final Duration COOLDOWN = Duration.ofSeconds(30);
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEA = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final SignedTexture SIGNED = new SignedTexture("value", "signature");

    @TempDir
    Path folder;

    private final MutableClock clock = new MutableClock();
    private final Deque<SignResult> answers = new ArrayDeque<>();
    private final List<BufferedImage> uploads = new ArrayList<>();
    private final List<String> heard = new ArrayList<>();
    private final List<WearerKind> kinds = new ArrayList<>();
    private TextureCache cache;
    private RenderQueue queue;

    @BeforeEach
    void setUp() {
        cache = YamlTextureCache.load(folder.resolve("texture-cache.yml"));
        queue = load();
    }

    @Test
    void signsAJobAndCachesTheTexture() {
        answers.add(new SignResult.Signed(SIGNED));
        RenderJob job = job(ALEX, 1);
        queue.submit(job);

        queue.runNext();

        assertEquals(List.of("signed " + ALEX + " value"), heard);
        assertEquals(List.of(WearerKind.PLAYER), kinds);
        assertEquals(SIGNED, cache.find(job.hash()).orElseThrow());
        assertFalse(queue.isWaiting(ALEX));
    }

    @Test
    void appliesACachedSkinWithoutUploading() throws IOException {
        RenderJob job = job(ALEX, 1);
        cache.put(job.hash(), SIGNED);
        queue.submit(job);

        queue.runNext();

        assertTrue(uploads.isEmpty());
        assertEquals(List.of("signed " + ALEX + " value"), heard);
    }

    @Test
    void aNewerOutfitReplacesTheWaitingOneAndKeepsItsPlace() {
        queue.submit(job(ALEX, 1));
        queue.submit(job(BEA, 2));
        RenderJob newer = job(ALEX, 3);
        queue.submit(newer);

        assertEquals(List.of(newer.wearer(), BEA), queue.waiting().stream().map(RenderJob::wearer).toList());
        assertEquals(newer, queue.waiting().getFirst());
    }

    @Test
    void uploadsOneJobPerRun() {
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.submit(job(BEA, 2));

        queue.runNext();

        assertEquals(1, uploads.size());
        assertTrue(queue.isWaiting(BEA));
    }

    @Test
    void waitsTheCooldownBetweenOnePlayersUploads() {
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        queue.submit(job(ALEX, 2));

        clock.advance(COOLDOWN.minusSeconds(1));
        queue.runNext();
        assertEquals(1, uploads.size());

        clock.advance(Duration.ofSeconds(1));
        queue.runNext();
        assertEquals(2, uploads.size());
    }

    @Test
    void aPlayerOnCooldownDoesNotHoldUpOthers() {
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        queue.submit(job(ALEX, 2));
        queue.submit(job(BEA, 3));

        queue.runNext();

        assertFalse(queue.isWaiting(BEA));
        assertTrue(queue.isWaiting(ALEX));
    }

    @Test
    void aRateLimitPausesEveryUploadAndKeepsTheJob() {
        answers.add(new SignResult.RateLimited(Duration.ofSeconds(10)));
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        queue.submit(job(BEA, 2));

        clock.advance(Duration.ofSeconds(9));
        queue.runNext();
        assertEquals(1, uploads.size());
        assertTrue(queue.isWaiting(ALEX));

        clock.advance(Duration.ofSeconds(1));
        queue.runNext();
        assertEquals(List.of("signed " + ALEX + " value"), heard);
    }

    @Test
    void aFailureIsReportedAndTheJobDropped() {
        answers.add(new SignResult.Failed("bad image"));
        queue.submit(job(ALEX, 1));

        queue.runNext();

        assertEquals(List.of("failed " + ALEX + " bad image"), heard);
        assertFalse(queue.isWaiting(ALEX));
    }

    @Test
    void waitingJobsSurviveARestart() {
        RenderJob job = job(ALEX, 1);
        queue.submit(new RenderJob(BEA, WearerKind.PLAYER, job.skin(), ArmModel.SLIM, job.hash()));

        RenderJob restored = load().waiting().getFirst();

        assertEquals(BEA, restored.wearer());
        assertEquals(ArmModel.SLIM, restored.armModel());
        assertEquals(job.hash(), TextureCache.hash(restored.skin()));
    }

    @Test
    void aFinishedJobLeavesTheFile() {
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));

        queue.runNext();

        assertTrue(load().waiting().isEmpty());
    }

    @Test
    void playersGoBeforeMannequinsThatWaitedLonger() {
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        RenderJob dummy = job(BEA, 2);
        queue.submit(new RenderJob(BEA, WearerKind.MANNEQUIN, dummy.skin(), ArmModel.CLASSIC, dummy.hash()));
        queue.submit(job(ALEX, 1));

        queue.runNext();
        queue.runNext();

        assertEquals(List.of("signed " + ALEX + " value", "signed " + BEA + " value"), heard);
    }

    @Test
    void anNpcGoesBeforeAMannequinButAfterAPlayer() {
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        UUID npc = UUID.randomUUID();
        RenderJob mannequin = job(BEA, 2);
        RenderJob citizen = job(npc, 3);
        queue.submit(new RenderJob(BEA, WearerKind.MANNEQUIN, mannequin.skin(), ArmModel.CLASSIC, mannequin.hash()));
        queue.submit(new RenderJob(npc, WearerKind.NPC, citizen.skin(), ArmModel.CLASSIC, citizen.hash()));
        queue.submit(job(ALEX, 1));

        queue.runNext();
        queue.runNext();
        queue.runNext();

        assertEquals(List.of("signed " + ALEX + " value", "signed " + npc + " value", "signed " + BEA + " value"), heard);
    }

    @Test
    void aMannequinRunsWhenNoPlayerIsReady() {
        answers.add(new SignResult.Signed(SIGNED));
        answers.add(new SignResult.Signed(SIGNED));
        queue.submit(job(ALEX, 1));
        queue.runNext();
        queue.submit(job(ALEX, 3));
        RenderJob dummy = job(BEA, 2);
        queue.submit(new RenderJob(BEA, WearerKind.MANNEQUIN, dummy.skin(), ArmModel.CLASSIC, dummy.hash()));

        queue.runNext();

        assertEquals("signed " + BEA + " value", heard.getLast());
        assertTrue(queue.isWaiting(ALEX));
    }

    @Test
    void doesNothingWithNothingWaiting() {
        queue.runNext();

        assertTrue(uploads.isEmpty());
        assertTrue(heard.isEmpty());
    }

    private RenderQueue load() {
        SkinSigner signer = (skin, armModel) -> {
            uploads.add(skin);
            return answers.removeFirst();
        };
        RenderListener listener = new RenderListener() {
            @Override
            public void signed(UUID player, WearerKind kind, SignedTexture texture) {
                heard.add("signed " + player + " " + texture.value());
                kinds.add(kind);
            }

            @Override
            public void failed(UUID player, WearerKind kind, String reason) {
                heard.add("failed " + player + " " + reason);
            }
        };
        return RenderQueue.load(folder.resolve("render-queue.yml"), signer, cache, () -> COOLDOWN, clock, listener,
                Logger.getLogger("test"));
    }

    private static RenderJob job(UUID player, int pixel) {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        skin.setRGB(0, 0, 0xFF000000 | pixel);
        return RenderJob.of(player, skin, ArmModel.CLASSIC);
    }
}
