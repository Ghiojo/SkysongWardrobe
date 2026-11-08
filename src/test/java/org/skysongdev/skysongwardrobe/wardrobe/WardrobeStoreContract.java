package org.skysongdev.skysongwardrobe.wardrobe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link WardrobeStore} must do, whatever it saves to. Public, so the SQL stores'
 * tests in other packages extend it too.
 */
public abstract class WardrobeStoreContract {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    private WardrobeStore store;

    /** A store with nothing saved yet. */
    protected abstract WardrobeStore emptyStore() throws Exception;

    @BeforeEach
    public void createStore() throws Exception {
        store = emptyStore();
    }

    private static Map<String, byte[]> slots(String... slotThenText) {
        Map<String, byte[]> slots = new LinkedHashMap<>();
        for (int i = 0; i < slotThenText.length; i += 2) {
            slots.put(slotThenText[i], slotThenText[i + 1].getBytes());
        }
        return slots;
    }

    @Test
    public void aNewOwnerHasNothingSaved() throws IOException {
        assertTrue(store.read(OWNER).isEmpty());
    }

    @Test
    public void writtenSlotsReadBackByteForByte() throws IOException {
        store.write(OWNER, slots("coat", "red coat", "boots", "old boots"));

        Map<String, byte[]> read = store.read(OWNER);

        assertEquals(2, read.size());
        assertArrayEquals("red coat".getBytes(), read.get("coat"));
        assertArrayEquals("old boots".getBytes(), read.get("boots"));
    }

    @Test
    public void aWriteReplacesEverySlot() throws IOException {
        store.write(OWNER, slots("coat", "red coat", "boots", "old boots"));
        store.write(OWNER, slots("coat", "blue coat"));

        Map<String, byte[]> read = store.read(OWNER);

        assertEquals(1, read.size());
        assertArrayEquals("blue coat".getBytes(), read.get("coat"));
    }

    @Test
    public void ownersAreKeptApart() throws IOException {
        store.write(OWNER, slots("coat", "mine"));
        store.write(OTHER, slots("coat", "theirs"));

        assertArrayEquals("mine".getBytes(), store.read(OWNER).get("coat"));
        assertArrayEquals("theirs".getBytes(), store.read(OTHER).get("coat"));
    }

    @Test
    public void deletedWardrobesReadAsEmpty() throws IOException {
        store.write(OWNER, slots("coat", "mine"));
        store.write(OTHER, slots("coat", "theirs"));

        store.delete(OWNER, false);

        assertTrue(store.read(OWNER).isEmpty());
        assertEquals(1, store.read(OTHER).size());
    }

    @Test
    public void aWardrobeKeptAsideReadsAsEmpty() throws IOException {
        store.write(OWNER, slots("coat", "mine"));

        store.delete(OWNER, true);

        assertTrue(store.read(OWNER).isEmpty());
    }

    @Test
    public void deletingNothingIsFine() throws IOException {
        store.delete(OWNER, false);
        store.delete(OWNER, true);

        assertTrue(store.read(OWNER).isEmpty());
    }
}
