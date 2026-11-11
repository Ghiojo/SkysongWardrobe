package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongwardrobe.api.Wearer;
import org.skysongdev.skysongwardrobe.api.event.OutfitRenderedEvent;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.TaskRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderEventsTest {
    private static final UUID WEARER = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private ServerMock server;
    private final List<Runnable> nextTick = new ArrayList<>();
    private final List<OutfitRenderedEvent> fired = new ArrayList<>();
    private RenderEvents events;

    /** Holds sync tasks until the test runs the next tick. */
    private final TaskRunner ticks = new TaskRunner() {
        @Override
        public void async(Runnable task) {
            task.run();
        }

        @Override
        public void sync(Runnable task) {
            nextTick.add(task);
        }
    };

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onRendered(OutfitRenderedEvent event) {
                fired.add(event);
            }
        }, MockBukkit.createMockPlugin());
        events = new RenderEvents(server, ticks);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void tick() {
        List<Runnable> now = new ArrayList<>(nextTick);
        nextTick.clear();
        now.forEach(Runnable::run);
    }

    @Test
    void aSignedSkinIsAnnouncedTheTickAfterTheApplierPutsItOn() {
        events.signed(WEARER, WearerKind.NPC, new SignedTexture("value", "signature"));

        tick();
        assertTrue(fired.isEmpty());
        tick();

        assertEquals(1, fired.size());
        assertEquals(Wearer.npc(WEARER), fired.get(0).getWearer());
        assertTrue(fired.get(0).isSuccess());
        assertNull(fired.get(0).getFailure());
    }

    @Test
    void aFailedSigningIsAnnouncedWithItsReason() {
        events.failed(WEARER, WearerKind.MANNEQUIN, "rate limited");
        tick();
        tick();

        assertEquals(Wearer.mannequin(WEARER), fired.get(0).getWearer());
        assertFalse(fired.get(0).isSuccess());
        assertEquals("rate limited", fired.get(0).getFailure());
    }
}
