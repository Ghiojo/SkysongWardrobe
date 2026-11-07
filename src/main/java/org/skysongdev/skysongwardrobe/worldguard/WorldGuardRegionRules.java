package org.skysongdev.skysongwardrobe.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * {@link RegionRules} backed by WorldGuard. Each action is checked like building: region members
 * are allowed, others only where the action's flag is set to allow, and WorldGuard's region
 * bypass permission always allows. Only touch this class when WorldGuard is installed.
 */
public final class WorldGuardRegionRules implements RegionRules {
    private static final Map<MannequinAction, StateFlag> FLAGS = new EnumMap<>(MannequinAction.class);

    /** Registers the flags. WorldGuard only takes new flags while plugins load, so call it from onLoad. */
    public static void registerFlags(Logger logger) {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        for (MannequinAction action : MannequinAction.values()) {
            try {
                StateFlag flag = new StateFlag(action.flagName(), false);
                registry.register(flag);
                FLAGS.put(action, flag);
            } catch (FlagConflictException taken) {
                Flag<?> existing = registry.get(action.flagName());
                if (existing instanceof StateFlag state) {
                    FLAGS.put(action, state);
                } else {
                    logger.warning("Another plugin owns the WorldGuard flag " + action.flagName()
                            + ", so regions can't control mannequins with it.");
                }
            }
        }
    }

    @Override
    public Optional<Boolean> allows(Player player, Location location, MannequinAction action) {
        com.sk89q.worldedit.util.Location at = BukkitAdapter.adapt(location);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(at);
        if (regions.size() == 0) {
            return Optional.empty();
        }
        LocalPlayer local = WorldGuardPlugin.inst().wrapPlayer(player);
        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(local, local.getWorld())) {
            return Optional.of(true);
        }
        StateFlag flag = FLAGS.get(action);
        return Optional.of(flag == null ? query.testBuild(at, local) : query.testBuild(at, local, flag));
    }
}
