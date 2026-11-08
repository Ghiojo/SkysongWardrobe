package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcRepositoryTest {
    private static final SignedTexture STEVE = new SignedTexture("steve-value", "steve-signature");
    private static final SignedTexture ALEX = new SignedTexture("alex-value", "alex-signature");

    @TempDir
    Path folder;

    private NpcRepository load() {
        return new NpcRepository(folder, Logger.getAnonymousLogger());
    }

    @Test
    void aSavedBaseSkinComesBackAfterARestart() throws IOException {
        UUID id = UUID.randomUUID();
        load().save(new NpcRecord(id, STEVE));

        assertEquals(Optional.of(new NpcRecord(id, STEVE)), load().find(id));
    }

    @Test
    void anNpcWithoutAFileHasNoRecord() {
        assertEquals(Optional.empty(), load().find(UUID.randomUUID()));
    }

    @Test
    void savingAgainReplacesTheBase() throws IOException {
        UUID id = UUID.randomUUID();
        NpcRepository repository = load();
        repository.save(new NpcRecord(id, STEVE));

        repository.save(repository.find(id).orElseThrow().withBase(ALEX));

        assertEquals(ALEX, repository.find(id).orElseThrow().base());
        assertEquals(ALEX, load().find(id).orElseThrow().base());
    }

    @Test
    void deletingRemovesTheFile() throws IOException {
        UUID id = UUID.randomUUID();
        NpcRepository repository = load();
        repository.save(new NpcRecord(id, STEVE));

        repository.delete(id);

        assertFalse(Files.exists(folder.resolve(id + ".yml")));
        assertEquals(Optional.empty(), repository.find(id));
    }

    @Test
    void aCopyKeepsTheBaseUnderTheNewId() {
        UUID copy = UUID.randomUUID();

        assertEquals(new NpcRecord(copy, STEVE), new NpcRecord(UUID.randomUUID(), STEVE).copyFor(copy));
    }

    @Test
    void aBrokenFileIsKeptAndTreatedAsMissing() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = folder.resolve(id + ".yml");
        Files.writeString(file, "base:\n  value: only-a-value\n");

        assertEquals(Optional.empty(), load().find(id));
        assertTrue(Files.exists(file));
    }

    @Test
    void forgettingReadsTheDiskAgain() throws IOException {
        UUID id = UUID.randomUUID();
        NpcRepository repository = load();
        repository.save(new NpcRecord(id, STEVE));
        Files.delete(folder.resolve(id + ".yml"));

        repository.forget(id);

        assertEquals(Optional.empty(), repository.find(id));
    }
}
