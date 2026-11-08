package org.skysongdev.skysongwardrobe.wardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongwardrobe.storage.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeRepositoryTest {
    private static final UUID OWNER = UUID.randomUUID();

    @TempDir
    Path folder;
    private WardrobeRepository repository;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        repository = new WardrobeRepository(folder, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void newOwnerHasAnEmptyWardrobeAndNoFile() {
        assertTrue(repository.load(OWNER).items().isEmpty());
        assertTrue(Files.notExists(folder.resolve(OWNER + ".yml")));
    }

    @Test
    void savedItemsComeBackAfterAFreshLoad() throws IOException {
        ItemStack coat = new ItemStack(Material.LEATHER_CHESTPLATE);
        coat.editMeta(meta -> meta.displayName(Component.text("Winter coat")));
        Wardrobe wardrobe = repository.load(OWNER);
        wardrobe.put("coat", coat);
        repository.save(wardrobe);

        repository.unload(OWNER);
        ItemStack loaded = repository.load(OWNER).item("coat").orElseThrow();

        assertEquals(coat, loaded);
        assertEquals(OWNER.toString(),
                YamlConfiguration.loadConfiguration(folder.resolve(OWNER + ".yml").toFile()).getString("owner"));
    }

    @Test
    void loadIsCachedUntilUnloaded() {
        Wardrobe first = repository.load(OWNER);

        assertSame(first, repository.load(OWNER));
        repository.unload(OWNER);
        assertNotSame(first, repository.load(OWNER));
    }

    @Test
    void unreadableItemsSurviveASave() throws IOException {
        Files.writeString(folder.resolve(OWNER + ".yml"), "owner: " + OWNER + "\nslots:\n  coat: \"not base64!\"\n");

        Wardrobe wardrobe = repository.load(OWNER);
        wardrobe.put("boots", new ItemStack(Material.LEATHER_BOOTS));
        repository.save(wardrobe);

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(folder.resolve(OWNER + ".yml").toFile());
        assertEquals("not base64!", saved.getString("slots.coat"));
        assertTrue(saved.isString("slots.boots"));
    }

    @Test
    void itemsThatCantBeDeserializedSurviveASaveAndKeepADeleteAside() throws IOException {
        Files.writeString(folder.resolve(OWNER + ".yml"), "owner: " + OWNER + "\nslots:\n  coat: \"AAECAw==\"\n");

        Wardrobe wardrobe = repository.load(OWNER);
        assertTrue(wardrobe.items().isEmpty());
        wardrobe.put("boots", new ItemStack(Material.LEATHER_BOOTS));
        repository.save(wardrobe);
        assertEquals("AAECAw==", YamlConfiguration.loadConfiguration(folder.resolve(OWNER + ".yml").toFile())
                .getString("slots.coat"));

        repository.delete(OWNER);
        assertTrue(Files.isRegularFile(folder.resolve(OWNER + ".broken.yml")));
    }

    @Test
    void aStoreThatCantBeReadIsAnErrorNotAnEmptyWardrobe() {
        WardrobeRepository failing = new WardrobeRepository(new WardrobeStore() {
            @Override
            public Map<String, byte[]> read(UUID owner) throws IOException {
                throw new IOException("database down");
            }

            @Override
            public void write(UUID owner, Map<String, byte[]> slots) {
            }

            @Override
            public void delete(UUID owner, boolean keepAside) {
            }
        }, Logger.getAnonymousLogger());

        assertThrows(StorageException.class, () -> failing.load(OWNER));
    }
}
