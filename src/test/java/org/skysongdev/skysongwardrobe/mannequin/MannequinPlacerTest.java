package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.items.FakeItemTags;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinPlacerTest {
    @TempDir
    Path folder;

    private SkysongWardrobe plugin;
    private WorldMock world;
    private PlayerMock player;
    private MannequinRepository repository;
    private MannequinMarker marker;
    private final List<Mannequin> placed = new ArrayList<>();
    private final FakeItemTags tags = new FakeItemTags().spawner(Material.ARMOR_STAND, "oak", "slim");
    private MannequinPlacer placer;
    private Block ground;
    private boolean regionAllows = true;
    private boolean spawnBlocked;
    private final MannequinAccess access = new MannequinAccess() {
        @Override
        public boolean canPlace(org.bukkit.entity.Player player, Location location) {
            return regionAllows;
        }

        @Override
        public boolean canUse(org.bukkit.entity.Player player, Mannequin mannequin, Location location) {
            return true;
        }

        @Override
        public boolean canBreak(org.bukkit.entity.Player player, Mannequin mannequin, Location location) {
            return true;
        }
    };

    @BeforeEach
    void setUp() throws IOException {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.addAttachment(plugin, "skysongwardrobe.mannequin", true);
        Path skins = plugin.getDataFolder().toPath().resolve("mannequin-skins");
        Files.createDirectories(skins);
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", skins.resolve("oak.png").toFile());
        plugin.configuration().reload();

        repository = MannequinRepository.load(folder, Logger.getAnonymousLogger());
        marker = new MannequinMarker(plugin);
        MannequinSpawner zombies = (location, model) -> {
            Zombie zombie = location.getWorld().spawn(location, Zombie.class);
            if (spawnBlocked) {
                zombie.remove();
            }
            return zombie;
        };
        placer = new MannequinPlacer(repository, marker, zombies, access, () -> tags, plugin.configuration(), placed::add,
                Logger.getAnonymousLogger());
        ground = world.getBlockAt(0, 64, 0);
        ground.setType(Material.STONE);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void setConfig(String yaml) throws IOException {
        Files.writeString(new File(plugin.getDataFolder(), "config.yml").toPath(), yaml);
        plugin.configuration().reload();
    }

    @Test
    void placesAMannequinAndKeepsOneSpawner() {
        ItemStack stack = new ItemStack(Material.ARMOR_STAND, 2);

        assertEquals(MannequinPlacer.Result.PLACED, placer.place(player, stack, ground));

        assertEquals(1, stack.getAmount());
        Mannequin mannequin = placed.getFirst();
        assertEquals(player.getUniqueId(), mannequin.owner());
        assertEquals("oak", mannequin.skin());
        assertEquals(ArmModel.SLIM, mannequin.model());
        assertEquals(1, mannequin.spawner().getAmount());
        assertTrue(repository.find(mannequin.id()).isPresent());
        LivingEntity entity = (LivingEntity) world.getEntity(mannequin.id());
        assertTrue(marker.isMarked(entity));
        assertEquals(new Location(world, 0.5, 65, 0.5).toVector(), entity.getLocation().toVector());
    }

    @Test
    void otherItemsAreNotSpawners() {
        assertEquals(MannequinPlacer.Result.NOT_A_SPAWNER, placer.place(player, new ItemStack(Material.STICK), ground));
        assertEquals(MannequinPlacer.Result.NOT_A_SPAWNER, placer.place(player, null, ground));
    }

    @Test
    void needsAMannequinPermission() {
        PlayerMock stranger = MockBukkit.getMock().addPlayer();

        assertEquals(MannequinPlacer.Result.NO_PERMISSION, placer.place(stranger, new ItemStack(Material.ARMOR_STAND), ground));
    }

    @Test
    void refusesASkinThatIsNotInTheFolder() {
        tags.spawner(Material.OAK_PLANKS, "marble", "classic");

        assertEquals(MannequinPlacer.Result.UNKNOWN_SKIN, placer.place(player, new ItemStack(Material.OAK_PLANKS), ground));
    }

    @Test
    void needsTwoFreeBlocks() {
        world.getBlockAt(0, 66, 0).setType(Material.STONE);

        assertEquals(MannequinPlacer.Result.NO_SPACE, placer.place(player, new ItemStack(Material.ARMOR_STAND), ground));
        assertTrue(placed.isEmpty());
    }

    @Test
    void keepsTheSpawnerWhenAnotherPluginStopsTheSpawn() {
        spawnBlocked = true;
        ItemStack item = new ItemStack(Material.ARMOR_STAND);

        assertEquals(MannequinPlacer.Result.NO_ACCESS_HERE, placer.place(player, item, ground));
        assertEquals(1, item.getAmount());
        assertTrue(placed.isEmpty());
    }

    @Test
    void respectsWhereTheRegionsSayNo() {
        regionAllows = false;

        assertEquals(MannequinPlacer.Result.NO_ACCESS_HERE, placer.place(player, new ItemStack(Material.ARMOR_STAND), ground));
        assertTrue(placed.isEmpty());
    }

    @Test
    void stopsAtThePerPlayerLimitUnlessStaff() throws IOException {
        setConfig("mannequins:\n  max-per-player: 1\n");
        placer.place(player, new ItemStack(Material.ARMOR_STAND), ground);

        assertEquals(MannequinPlacer.Result.PLAYER_LIMIT,
                placer.place(player, new ItemStack(Material.ARMOR_STAND), world.getBlockAt(3, 64, 0)));

        player.addAttachment(plugin, "skysongwardrobe.mannequin.other", true);
        assertEquals(MannequinPlacer.Result.PLACED,
                placer.place(player, new ItemStack(Material.ARMOR_STAND), world.getBlockAt(3, 64, 0)));
    }

    @Test
    void stopsAtThePerChunkLimit() throws IOException {
        setConfig("mannequins:\n  max-per-chunk: 1\n");
        placer.place(player, new ItemStack(Material.ARMOR_STAND), ground);

        assertEquals(MannequinPlacer.Result.CHUNK_LIMIT,
                placer.place(player, new ItemStack(Material.ARMOR_STAND), world.getBlockAt(5, 64, 5)));
        assertEquals(MannequinPlacer.Result.PLACED,
                placer.place(player, new ItemStack(Material.ARMOR_STAND), world.getBlockAt(40, 64, 0)));
    }

    @Test
    void facesBackAtThePlayerInEightDirections() {
        assertEquals(180f, MannequinPlacer.facing(0f));
        assertEquals(0f, MannequinPlacer.facing(170f));
        assertEquals(225f, MannequinPlacer.facing(50f));
        assertEquals(90f, MannequinPlacer.facing(-90f));
    }
}
