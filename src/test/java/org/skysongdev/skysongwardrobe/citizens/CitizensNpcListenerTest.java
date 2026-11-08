package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.event.NPCCloneEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.npc.NPC;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.npc.NpcLifecycle;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitizensNpcListenerTest {
    private final NpcLifecycle lifecycle = mock(NpcLifecycle.class);
    private final CitizensNpcListener listener = new CitizensNpcListener(lifecycle);

    private static NPC npc(UUID id, int number, String name) {
        NPC npc = mock(NPC.class);
        when(npc.getUniqueId()).thenReturn(id);
        when(npc.getId()).thenReturn(number);
        when(npc.getName()).thenReturn(name);
        return npc;
    }

    @Test
    void aRemovedNpcIsArchived() {
        UUID id = UUID.randomUUID();

        listener.onRemove(new NPCRemoveEvent(npc(id, 3, "Baker")));

        verify(lifecycle).removed(new NpcInfo(id, 3, "Baker", Optional.empty()));
    }

    @Test
    void aCopiedNpcGetsTheOriginalsWardrobe() {
        UUID original = UUID.randomUUID();
        UUID copy = UUID.randomUUID();

        listener.onCopy(new NPCCloneEvent(npc(original, 3, "Baker"), npc(copy, 4, "Baker")));

        verify(lifecycle).copied(original, copy);
    }
}
