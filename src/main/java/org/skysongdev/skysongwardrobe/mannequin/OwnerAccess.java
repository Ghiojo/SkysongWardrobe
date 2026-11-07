package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.config.MannequinSettings;

import java.util.function.Supplier;

/** Only a mannequin's owner, or staff with an {@code .other} mannequin permission, may use or break it. */
public final class OwnerAccess implements MannequinAccess {
    private final Supplier<MannequinSettings> settings;

    public OwnerAccess(Supplier<MannequinSettings> settings) {
        this.settings = settings;
    }

    @Override
    public boolean canUse(Player player, Mannequin mannequin) {
        return isOwnerOrStaff(player, mannequin);
    }

    @Override
    public boolean canBreak(Player player, Mannequin mannequin) {
        return isOwnerOrStaff(player, mannequin);
    }

    private boolean isOwnerOrStaff(Player player, Mannequin mannequin) {
        return player.getUniqueId().equals(mannequin.owner())
                || Permissions.hasAnyOther(player, settings.get().permissions());
    }
}
