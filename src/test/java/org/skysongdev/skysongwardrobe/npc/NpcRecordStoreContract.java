package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What every {@link NpcRecordStore} must do. Public, so the SQL store's tests extend it too. */
public abstract class NpcRecordStoreContract {
    private NpcRecordStore store;

    protected abstract NpcRecordStore emptyStore() throws Exception;

    @BeforeEach
    public void createStore() throws Exception {
        store = emptyStore();
    }

    @Test
    public void anUnknownNpcHasNoRecord() throws IOException {
        assertTrue(store.read(UUID.randomUUID()).isEmpty());
    }

    @Test
    public void aWrittenRecordReadsBack() throws IOException {
        NpcRecord record = new NpcRecord(UUID.randomUUID(), new SignedTexture("value", "signature"));
        store.write(record);

        assertEquals(Optional.of(record), store.read(record.id()));
    }

    @Test
    public void writingAgainReplacesTheBaseSkin() throws IOException {
        NpcRecord record = new NpcRecord(UUID.randomUUID(), new SignedTexture("old", "old-signature"));
        store.write(record);
        NpcRecord changed = record.withBase(new SignedTexture("new", "new-signature"));
        store.write(changed);

        assertEquals(Optional.of(changed), store.read(record.id()));
    }

    @Test
    public void deleteRemovesOnlyThatNpc() throws IOException {
        NpcRecord kept = new NpcRecord(UUID.randomUUID(), new SignedTexture("kept", "kept-signature"));
        NpcRecord removed = new NpcRecord(UUID.randomUUID(), new SignedTexture("gone", "gone-signature"));
        store.write(kept);
        store.write(removed);

        store.delete(removed.id());
        store.delete(UUID.randomUUID());

        assertTrue(store.read(removed.id()).isEmpty());
        assertEquals(Optional.of(kept), store.read(kept.id()));
    }
}
