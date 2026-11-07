package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.config.MannequinSettings;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Places a mannequin from a spawner item onto the top of a block: checks permission, skin,
 * space, {@link MannequinAccess} (regions) and limits, spawns it facing the player, saves it with one spawner item taken from
 * the stack, and then hands it to {@code placed} (which renders its skin).
 */
public final class MannequinPlacer {
    /** How a placing attempt ended; every result but {@link #NOT_A_SPAWNER} uses up the click. */
    public enum Result { NOT_A_SPAWNER, NO_PERMISSION, UNKNOWN_SKIN, NO_SPACE, NO_ACCESS_HERE, PLAYER_LIMIT, CHUNK_LIMIT, SAVE_ERROR, PLACED }

    /** Facing is rounded to the nearest of eight directions. */
    private static final float FACING_STEP = 45f;

    private final MannequinRepository mannequins;
    private final MannequinMarker marker;
    private final MannequinSpawner spawner;
    private final MannequinAccess access;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Consumer<Mannequin> placed;
    private final Logger logger;

    public MannequinPlacer(MannequinRepository mannequins, MannequinMarker marker, MannequinSpawner spawner,
                           MannequinAccess access, Supplier<ItemTagGateway> tags, PluginConfiguration configuration,
                           Consumer<Mannequin> placed, Logger logger) {
        this.mannequins = mannequins;
        this.marker = marker;
        this.spawner = spawner;
        this.access = access;
        this.tags = tags;
        this.configuration = configuration;
        this.placed = placed;
        this.logger = logger;
    }

    /** Tries to place the item in the player's hand on top of {@code ground}. Main thread only. */
    public Result place(Player player, ItemStack item, Block ground) {
        ItemTagGateway gateway = tags.get();
        if (item == null || item.isEmpty() || !gateway.isSkysongItem(item)) {
            return Result.NOT_A_SPAWNER;
        }
        Optional<String> skin = gateway.mannequinSkin(item);
        if (skin.isEmpty()) {
            return Result.NOT_A_SPAWNER;
        }
        MannequinSettings settings = configuration.settings().mannequins();
        if (!Permissions.hasAny(player, settings.permissions())) {
            return Result.NO_PERMISSION;
        }
        if (!configuration.mannequinSkins().has(skin.get())) {
            return Result.UNKNOWN_SKIN;
        }
        Block feet = ground.getRelative(BlockFace.UP);
        if (!feet.isPassable() || !feet.getRelative(BlockFace.UP).isPassable()) {
            return Result.NO_SPACE;
        }
        if (!access.canPlace(player, feet.getLocation())) {
            return Result.NO_ACCESS_HERE;
        }
        boolean staff = Permissions.hasAnyOther(player, settings.permissions());
        if (!staff && !MannequinSettings.withinLimit(settings.perPlayer(), mannequins.countOwnedBy(player.getUniqueId()))) {
            return Result.PLAYER_LIMIT;
        }
        if (!MannequinSettings.withinLimit(settings.perChunk(), countIn(feet))) {
            return Result.CHUNK_LIMIT;
        }
        return spawn(player, item, feet, skin.get(), ArmModel.fromTag(gateway.mannequinModel(item).orElse(null)));
    }

    private Result spawn(Player player, ItemStack item, Block feet, String skin, ArmModel model) {
        Location location = feet.getLocation().add(0.5, 0, 0.5);
        location.setYaw(facing(player.getLocation().getYaw()));
        LivingEntity entity = spawner.spawn(location, model);
        if (!entity.isValid()) {
            // Another plugin cancelled the spawn.
            return Result.NO_ACCESS_HERE;
        }
        marker.mark(entity);
        Mannequin mannequin = new Mannequin(entity.getUniqueId(), player.getUniqueId(), item, skin, model);
        try {
            mannequins.save(mannequin);
        } catch (IOException failed) {
            logger.log(Level.SEVERE, "Could not save a new mannequin, so it was not placed.", failed);
            entity.remove();
            return Result.SAVE_ERROR;
        }
        item.subtract();
        placed.accept(mannequin);
        return Result.PLACED;
    }

    private int countIn(Block feet) {
        Entity[] entities = feet.getChunk().getEntities();
        return (int) Arrays.stream(entities).filter(marker::isMarked).count();
    }

    /** The yaw that faces back at a player looking along {@code playerYaw}, in steps of 45 degrees. */
    static float facing(float playerYaw) {
        float towardPlayer = playerYaw + 180f;
        float snapped = Math.round(towardPlayer / FACING_STEP) * FACING_STEP;
        return ((snapped % 360f) + 360f) % 360f;
    }
}
