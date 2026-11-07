package org.skysongdev.skysongwardrobe.mannequin;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinRepositoryTest {
    private static final UUID ALEX = UUID.randomUUID();
    private static final UUID BEA = UUID.randomUUID();

    @TempDir
    Path folder;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private MannequinRepository load() {
        return MannequinRepository.load(folder, Logger.getAnonymousLogger());
    }

    private static Mannequin mannequin(UUID owner) {
        ItemStack spawner = new ItemStack(Material.ARMOR_STAND, 3);
        spawner.editMeta(meta -> meta.displayName(Component.text("Oak mannequin")));
        return new Mannequin(UUID.randomUUID(), owner, spawner, "oak", ArmModel.SLIM);
    }

    @Test
    void savedMannequinsComeBackAfterARestart() throws IOException {
        Mannequin saved = mannequin(ALEX);
        load().save(saved);

        Mannequin loaded = load().find(saved.id()).orElseThrow();

        assertEquals(saved.owner(), loaded.owner());
        assertEquals("oak", loaded.skin());
        assertEquals(ArmModel.SLIM, loaded.model());
        assertEquals(saved.spawner(), loaded.spawner());
    }

    @Test
    void keepsOneSpawnerItemAndHandsOutCopies() {
        Mannequin mannequin = mannequin(ALEX);

        assertEquals(1, mannequin.spawner().getAmount());
        assertNotSame(mannequin.spawner(), mannequin.spawner());
    }

    @Test
    void countsMannequinsPerOwner() throws IOException {
        MannequinRepository repository = load();
        repository.save(mannequin(ALEX));
        repository.save(mannequin(ALEX));
        repository.save(mannequin(BEA));

        assertEquals(2, load().countOwnedBy(ALEX));
        assertEquals(0, load().countOwnedBy(UUID.randomUUID()));
    }

    @Test
    void deleteRemovesTheFile() throws IOException {
        MannequinRepository repository = load();
        Mannequin mannequin = mannequin(ALEX);
        repository.save(mannequin);

        repository.delete(mannequin.id());

        assertTrue(repository.find(mannequin.id()).isEmpty());
        assertFalse(repository.isKnown(mannequin.id()));
        assertTrue(Files.notExists(folder.resolve(mannequin.id() + ".yml")));
    }

    @Test
    void anUnreadableFileIsKnownButUntouched() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = folder.resolve(id + ".yml");
        Files.writeString(file, "owner: nobody\nspawner: broken\n");

        MannequinRepository repository = load();

        assertTrue(repository.isKnown(id));
        assertTrue(repository.find(id).isEmpty());
        assertEquals("owner: nobody\nspawner: broken\n", Files.readString(file));
    }

    @Test
    void ignoresFilesThatAreNotNamedByAUuid() throws IOException {
        Files.writeString(folder.resolve("notes.yml"), "hello: world\n");

        assertEquals(0, load().countOwnedBy(ALEX));
    }

    @Test
    void aMissingFolderHasNoMannequins() {
        MannequinRepository repository = MannequinRepository.load(folder.resolve("missing"), Logger.getAnonymousLogger());

        assertFalse(repository.isKnown(UUID.randomUUID()));
    }
}
