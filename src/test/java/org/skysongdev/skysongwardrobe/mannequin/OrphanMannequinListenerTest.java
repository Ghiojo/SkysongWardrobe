package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrphanMannequinListenerTest {
    @TempDir
    Path folder;

    private WorldMock world;
    private MannequinMarker marker;
    private MannequinRepository repository;
    private OrphanMannequinListener listener;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongWardrobe plugin = MockBukkit.load(SkysongWardrobe.class);
        world = server.addSimpleWorld("world");
        marker = new MannequinMarker(plugin);
        repository = MannequinRepository.load(folder, Logger.getAnonymousLogger());
        listener = new OrphanMannequinListener(repository, marker, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Entity spawn(boolean marked) {
        Zombie entity = world.spawn(world.getSpawnLocation(), Zombie.class);
        if (marked) {
            marker.mark(entity);
        }
        return entity;
    }

    private void load(Entity entity) {
        listener.onLoad(new EntitiesLoadEvent(entity.getLocation().getChunk(), List.of(entity)));
    }

    @Test
    void removesAMarkedEntityWithoutAFile() {
        Entity orphan = spawn(true);

        load(orphan);

        assertFalse(orphan.isValid());
    }

    @Test
    void keepsAMannequinWithAFile() throws IOException {
        Entity known = spawn(true);
        repository.save(new Mannequin(known.getUniqueId(), known.getUniqueId(), new ItemStack(Material.STICK), "oak",
                ArmModel.CLASSIC));

        load(known);

        assertTrue(known.isValid());
    }

    @Test
    void keepsAMannequinWhoseFileCantBeRead() throws IOException {
        Entity unreadable = spawn(true);
        Files.writeString(folder.resolve(unreadable.getUniqueId() + ".yml"), "spawner: broken\n");
        repository = MannequinRepository.load(folder, Logger.getAnonymousLogger());
        listener = new OrphanMannequinListener(repository, marker, Logger.getAnonymousLogger());

        load(unreadable);

        assertTrue(unreadable.isValid());
    }

    @Test
    void leavesOtherEntitiesAlone() {
        Entity zombie = spawn(false);

        load(zombie);

        assertTrue(zombie.isValid());
    }
}
