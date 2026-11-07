package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.scheduling.ImmediateTaskRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinSkinApplierTest {
    private static final SignedTexture SIGNED = new SignedTexture("outfit", "outfit-sig");

    private final FakeMannequinBodies bodies = new FakeMannequinBodies();
    private final MannequinSkinApplier applier = new MannequinSkinApplier(bodies, new ImmediateTaskRunner());
    private Entity entity;

    @BeforeEach
    void setUp() {
        WorldMock world = MockBukkit.mock().addSimpleWorld("world");
        entity = world.spawn(world.getSpawnLocation(), Zombie.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void showsTheSkinOnALoadedMannequin() {
        bodies.loaded.add(entity.getUniqueId());

        applier.signed(entity.getUniqueId(), WearerKind.MANNEQUIN, SIGNED);

        assertEquals(SIGNED, bodies.skins.get(entity.getUniqueId()));
    }

    @Test
    void anUnloadedMannequinGetsItsSkinWhenItsChunkLoads() {
        applier.signed(entity.getUniqueId(), WearerKind.MANNEQUIN, SIGNED);
        assertTrue(bodies.skins.isEmpty());

        bodies.loaded.add(entity.getUniqueId());
        applier.onLoad(new EntitiesLoadEvent(entity.getLocation().getChunk(), List.of(entity)));

        assertEquals(SIGNED, bodies.skins.get(entity.getUniqueId()));
    }

    @Test
    void aPendingSkinIsOnlyAppliedOnce() {
        applier.signed(entity.getUniqueId(), WearerKind.MANNEQUIN, SIGNED);
        bodies.loaded.add(entity.getUniqueId());
        applier.onLoad(new EntitiesLoadEvent(entity.getLocation().getChunk(), List.of(entity)));
        bodies.skins.clear();

        applier.onLoad(new EntitiesLoadEvent(entity.getLocation().getChunk(), List.of(entity)));

        assertTrue(bodies.skins.isEmpty());
    }
}
