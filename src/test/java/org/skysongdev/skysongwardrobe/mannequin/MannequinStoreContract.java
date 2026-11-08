package org.skysongdev.skysongwardrobe.mannequin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What every {@link MannequinStore} must do. Public, so the SQL store's tests extend it too. */
public abstract class MannequinStoreContract {
    private MannequinStore store;

    protected abstract MannequinStore emptyStore() throws Exception;

    @BeforeEach
    public void createStore() throws Exception {
        store = emptyStore();
    }

    private static MannequinStore.Row row(String skin) {
        return new MannequinStore.Row(UUID.randomUUID(), UUID.randomUUID(), skin, "SLIM", new byte[]{1, 2, 3});
    }

    @Test
    public void anEmptyStoreHasNoMannequins() throws IOException {
        MannequinStore.Contents contents = store.readAll();

        assertTrue(contents.rows().isEmpty());
        assertTrue(contents.unreadable().isEmpty());
    }

    @Test
    public void writtenMannequinsReadBack() throws IOException {
        MannequinStore.Row oak = row("oak");
        store.write(oak);
        store.write(row("birch"));

        MannequinStore.Row read = store.readAll().rows().stream()
                .filter(row -> row.id().equals(oak.id())).findFirst().orElseThrow();

        assertEquals(2, store.readAll().rows().size());
        assertEquals(oak.owner(), read.owner());
        assertEquals("oak", read.skin());
        assertEquals("SLIM", read.model());
        assertArrayEquals(new byte[]{1, 2, 3}, read.spawner());
    }

    @Test
    public void writingTheSameIdReplacesIt() throws IOException {
        MannequinStore.Row oak = row("oak");
        store.write(oak);
        store.write(new MannequinStore.Row(oak.id(), oak.owner(), "birch", "WIDE", new byte[]{4}));

        assertEquals(1, store.readAll().rows().size());
        assertEquals("birch", store.readAll().rows().getFirst().skin());
    }

    @Test
    public void deletedMannequinsAreGone() throws IOException {
        MannequinStore.Row oak = row("oak");
        store.write(oak);
        store.write(row("birch"));

        store.delete(oak.id());
        store.delete(UUID.randomUUID());

        assertEquals(1, store.readAll().rows().size());
        assertEquals("birch", store.readAll().rows().getFirst().skin());
    }
}
