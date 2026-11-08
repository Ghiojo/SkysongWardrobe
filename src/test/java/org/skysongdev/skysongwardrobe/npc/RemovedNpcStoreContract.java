package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What every {@link RemovedNpcStore} must do. Public, so the SQL store's tests extend it too. */
public abstract class RemovedNpcStoreContract {
    private static final Instant MONDAY = Instant.parse("2026-11-02T10:00:00Z");
    private RemovedNpcStore store;

    protected abstract RemovedNpcStore emptyStore() throws Exception;

    @BeforeEach
    public void createStore() throws Exception {
        store = emptyStore();
    }

    private static RemovedNpc entry(String name) {
        return new RemovedNpc(UUID.randomUUID(), UUID.randomUUID(), 7, name, MONDAY);
    }

    @Test
    public void anEmptyStoreHasNoEntries() throws IOException {
        assertTrue(store.readAll().isEmpty());
    }

    @Test
    public void writtenEntriesReadBack() throws IOException {
        RemovedNpc baker = entry("Baker");
        RemovedNpc guard = entry("Guard");
        store.write(baker);
        store.write(guard);

        assertEquals(Set.of(baker, guard), Set.copyOf(store.readAll()));
    }

    @Test
    public void writingAnEntryAgainReplacesIt() throws IOException {
        RemovedNpc baker = entry("Baker");
        store.write(baker);
        RemovedNpc renamed = new RemovedNpc(baker.entry(), baker.npc(), 9, "Head baker", MONDAY.plusSeconds(60));
        store.write(renamed);

        assertEquals(List.of(renamed), store.readAll());
    }

    @Test
    public void deleteRemovesOnlyThatEntry() throws IOException {
        RemovedNpc kept = entry("Baker");
        RemovedNpc gone = entry("Guard");
        store.write(kept);
        store.write(gone);

        store.delete(gone.entry());
        store.delete(UUID.randomUUID());

        assertEquals(List.of(kept), store.readAll());
    }
}
