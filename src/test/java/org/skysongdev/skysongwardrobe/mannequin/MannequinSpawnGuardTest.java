package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinSpawnGuardTest {
    private final MannequinSpawnGuard guard = new MannequinSpawnGuard();
    private WorldMock world;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void letsThroughTheMannequinBeingPlacedWhenAnotherPluginCancelsIt() {
        Zombie placed = world.spawn(new Location(world, 0, 64, 0), Zombie.class);
        CreatureSpawnEvent event = cancelledSpawn(placed);

        guard.allowWhile(() -> {
            guard.expect(placed.getUniqueId());
            guard.onSpawn(event);
            return placed;
        });

        assertFalse(event.isCancelled());
        assertTrue(guard.placing().isEmpty(), "the guard forgets the entity once the spawn is done");
    }

    @Test
    void leavesEveryOtherSpawnAlone() {
        Zombie placed = world.spawn(new Location(world, 0, 64, 0), Zombie.class);
        Zombie mob = world.spawn(new Location(world, 4, 64, 0), Zombie.class);
        CreatureSpawnEvent mobSpawn = cancelledSpawn(mob);
        CreatureSpawnEvent later = cancelledSpawn(placed);

        guard.allowWhile(() -> {
            guard.expect(placed.getUniqueId());
            guard.onSpawn(mobSpawn);
            return placed;
        });
        guard.onSpawn(later);

        assertTrue(mobSpawn.isCancelled());
        assertTrue(later.isCancelled());
    }

    private static CreatureSpawnEvent cancelledSpawn(Zombie entity) {
        CreatureSpawnEvent event = new CreatureSpawnEvent(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
        event.setCancelled(true);
        return event;
    }
}
