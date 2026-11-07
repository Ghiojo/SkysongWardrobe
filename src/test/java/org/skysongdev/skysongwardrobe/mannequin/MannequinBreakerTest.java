package org.skysongdev.skysongwardrobe.mannequin;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.skysongdev.skysongwardrobe.render.RenderJob;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.render.RenderRouter;
import org.skysongdev.skysongwardrobe.render.SignResult;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.WearerKind;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MannequinBreakerTest {
    @TempDir
    Path folder;

    private ServerMock server;
    private SkysongWardrobe plugin;
    private WorldMock world;
    private PlayerMock owner;
    private MannequinMarker marker;
    private MannequinRepository mannequins;
    private WardrobeRepository wardrobes;
    private RenderQueue queue;
    private Entity entity;
    private MannequinBreakListener listener;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        world = server.addSimpleWorld("world");
        owner = server.addPlayer();
        marker = new MannequinMarker(plugin);
        mannequins = MannequinRepository.load(folder, Logger.getAnonymousLogger());
        wardrobes = new WardrobeRepository(folder.resolve("wardrobes"), Logger.getAnonymousLogger());
        queue = RenderQueue.load(folder.resolve("render-queue.yml"), (skin, arms) -> new SignResult.Signed(
                new SignedTexture("v", "s")), TextureCache.load(folder.resolve("cache.yml")), () -> Duration.ZERO,
                Clock.systemUTC(), new RenderRouter(), Logger.getAnonymousLogger());
        entity = world.spawn(world.getSpawnLocation(), Zombie.class);
        marker.mark(entity);
        mannequins.save(new Mannequin(entity.getUniqueId(), owner.getUniqueId(), new ItemStack(Material.ARMOR_STAND),
                "oak", ArmModel.CLASSIC));
        MannequinBreaker breaker = new MannequinBreaker(mannequins, wardrobes,
                new OwnerAccess(() -> plugin.configuration().settings().mannequins()), queue, server,
                Logger.getAnonymousLogger());
        listener = new MannequinBreakListener(breaker, marker, plugin.configuration()::messages);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PrePlayerAttackEntityEvent punch(PlayerMock player, boolean sneaking) {
        player.setSneaking(sneaking);
        PrePlayerAttackEntityEvent event = new PrePlayerAttackEntityEvent(player, entity, true);
        listener.onAttack(event);
        return event;
    }

    private List<Material> drops() {
        return world.getEntitiesByClass(Item.class).stream().map(item -> item.getItemStack().getType()).sorted().toList();
    }

    @Test
    void sneakPunchDropsTheSpawnerAndEverythingWorn() throws IOException {
        Wardrobe wardrobe = wardrobes.load(entity.getUniqueId());
        wardrobe.put("coat", new ItemStack(Material.LEATHER_CHESTPLATE));
        wardrobe.put("equipment:head", new ItemStack(Material.IRON_HELMET));
        wardrobes.save(wardrobe);

        assertTrue(punch(owner, true).isCancelled());

        assertFalse(entity.isValid());
        assertEquals(List.of(Material.ARMOR_STAND, Material.LEATHER_CHESTPLATE, Material.IRON_HELMET).stream().sorted().toList(),
                drops());
        assertTrue(mannequins.find(entity.getUniqueId()).isEmpty());
        assertTrue(Files.notExists(folder.resolve("wardrobes").resolve(entity.getUniqueId() + ".yml")));
    }

    @Test
    void aPlainPunchOnlyHints() {
        assertTrue(punch(owner, false).isCancelled());

        assertTrue(entity.isValid());
        assertTrue(drops().isEmpty());
    }

    @Test
    void someoneElseCantBreakIt() {
        PlayerMock stranger = server.addPlayer();

        punch(stranger, true);

        assertTrue(entity.isValid());
        assertEquals("[Wardrobe] This mannequin isn't yours.",
                PlainTextComponentSerializer.plainText().serialize(stranger.nextComponentMessage()));
    }

    @Test
    void breakingDropsItsWaitingRender() {
        queue.submit(RenderJob.of(entity.getUniqueId(), WearerKind.MANNEQUIN,
                new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), ArmModel.CLASSIC));

        punch(owner, true);

        assertFalse(queue.isWaiting(entity.getUniqueId()));
    }

    @Test
    void closesTheWardrobeOfWhoeverIsDressingIt() {
        WardrobeMenu menu = WardrobeMenu.forMannequin(wardrobes.load(entity.getUniqueId()),
                plugin.configuration().layout().orElseThrow(), new FakeItemTags());
        owner.openInventory(menu.getInventory());

        punch(owner, true);

        assertTrue(WardrobeMenu.of(owner.getOpenInventory()).isEmpty());
    }

    @Test
    void noOtherDamageReachesAMannequin() {
        EntityDamageEvent fire = new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.FIRE,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.IN_FIRE).build(), 5);

        listener.onDamage(fire);

        assertTrue(fire.isCancelled());
    }
}
