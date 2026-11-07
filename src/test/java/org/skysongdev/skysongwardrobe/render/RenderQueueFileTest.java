package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderQueueFileTest {
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEA = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @TempDir
    Path folder;

    @Test
    void savedJobsLoadBackInOrder() throws IOException {
        RenderJob first = job(BEA, ArmModel.SLIM, 1);
        RenderJob second = job(ALEX, ArmModel.CLASSIC, 2);
        file().save(List.of(first, second));

        List<RenderJob> loaded = file().load();

        assertEquals(List.of(BEA, ALEX), loaded.stream().map(RenderJob::player).toList());
        assertEquals(ArmModel.SLIM, loaded.getFirst().armModel());
        assertEquals(first.hash(), loaded.getFirst().hash());
        assertEquals(first.hash(), TextureCache.hash(loaded.getFirst().skin()));
    }

    @Test
    void savingNoJobsEmptiesTheFile() throws IOException {
        file().save(List.of(job(ALEX, ArmModel.CLASSIC, 1)));
        file().save(List.of());

        assertTrue(file().load().isEmpty());
    }

    @Test
    void aMissingFileHasNoJobs() {
        assertTrue(file().load().isEmpty());
    }

    @Test
    void unreadableEntriesAreSkippedAndTheRestKept() throws IOException {
        file().save(List.of(job(ALEX, ArmModel.CLASSIC, 1)));
        String saved = Files.readString(path());
        Files.writeString(path(), saved
                + "  not-a-uuid:\n    hash: x\n"
                + "  " + BEA + ":\n    arm-model: CLASSIC\n    hash: x\n    skin: bm90IGFuIGltYWdl\n");

        assertEquals(List.of(ALEX), file().load().stream().map(RenderJob::player).toList());
    }

    @Test
    void aJobHashesItsSkin() {
        RenderJob job = job(ALEX, ArmModel.CLASSIC, 1);

        assertEquals(TextureCache.hash(job.skin()), job.hash());
    }

    private RenderQueueFile file() {
        return new RenderQueueFile(path(), Logger.getLogger("test"));
    }

    private Path path() {
        return folder.resolve("render-queue.yml");
    }

    private static RenderJob job(UUID player, ArmModel armModel, int pixel) {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        skin.setRGB(0, 0, 0xFF000000 | pixel);
        return RenderJob.of(player, skin, armModel);
    }
}
