package org.skysongdev.skysongwardrobe.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.config.MannequinSettings;
import org.skysongdev.skysongwardrobe.mannequin.Mannequin;
import org.skysongdev.skysongwardrobe.mannequin.MannequinAccess;

import java.util.function.Supplier;

/**
 * Inside a region, its flags decide: members may place, use and break mannequins, and others
 * only where the matching flag is set to allow. Outside every region the {@code fallback}
 * (owner and staff) decides. Staff with an {@code .other} mannequin permission may always.
 */
public final class RegionAccess implements MannequinAccess {
    private final RegionRules regions;
    private final MannequinAccess fallback;
    private final Supplier<MannequinSettings> settings;

    public RegionAccess(RegionRules regions, MannequinAccess fallback, Supplier<MannequinSettings> settings) {
        this.regions = regions;
        this.fallback = fallback;
        this.settings = settings;
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return staff(player) || regions.allows(player, location, MannequinAction.PLACE)
                .orElseGet(() -> fallback.canPlace(player, location));
    }

    @Override
    public boolean canUse(Player player, Mannequin mannequin, Location location) {
        return staff(player) || regions.allows(player, location, MannequinAction.USE)
                .orElseGet(() -> fallback.canUse(player, mannequin, location));
    }

    @Override
    public boolean canBreak(Player player, Mannequin mannequin, Location location) {
        return staff(player) || regions.allows(player, location, MannequinAction.BREAK)
                .orElseGet(() -> fallback.canBreak(player, mannequin, location));
    }

    private boolean staff(Player player) {
        return Permissions.hasAnyOther(player, settings.get().permissions());
    }
}
