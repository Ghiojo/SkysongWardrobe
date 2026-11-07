package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Breaks a mannequin: closes its wardrobe for anyone dressing it, drops its saved spawner and
 * everything it wore at its feet, and forgets it everywhere (entity, file, wardrobe, render job).
 */
public final class MannequinBreaker {
    /** How a break attempt ended. */
    public enum Result { NOT_A_MANNEQUIN, NO_ACCESS, BROKEN }

    private final MannequinRepository mannequins;
    private final WardrobeRepository wardrobes;
    private final MannequinAccess access;
    private final RenderQueue queue;
    private final Server server;
    private final Logger logger;

    public MannequinBreaker(MannequinRepository mannequins, WardrobeRepository wardrobes, MannequinAccess access,
                            RenderQueue queue, Server server, Logger logger) {
        this.mannequins = mannequins;
        this.wardrobes = wardrobes;
        this.access = access;
        this.queue = queue;
        this.server = server;
        this.logger = logger;
    }

    /** Breaks the mannequin entity if the player may. Main thread only. */
    public Result breakBy(Player player, Entity entity) {
        Optional<Mannequin> found = mannequins.find(entity.getUniqueId());
        if (found.isEmpty()) {
            return Result.NOT_A_MANNEQUIN;
        }
        Mannequin mannequin = found.get();
        if (!access.canBreak(player, mannequin, entity.getLocation())) {
            return Result.NO_ACCESS;
        }
        closeMenus(mannequin);
        Location feet = entity.getLocation();
        List<ItemStack> drops = new ArrayList<>();
        drops.add(mannequin.spawner());
        drops.addAll(wardrobes.load(mannequin.id()).items().values());
        entity.remove();
        drops.forEach(item -> feet.getWorld().dropItemNaturally(feet, item));
        forget(mannequin);
        return Result.BROKEN;
    }

    private void closeMenus(Mannequin mannequin) {
        for (Player online : server.getOnlinePlayers()) {
            WardrobeMenu.of(online.getOpenInventory())
                    .filter(menu -> menu.isMannequin() && menu.wardrobe().owner().equals(mannequin.id()))
                    .ifPresent(menu -> online.closeInventory());
        }
    }

    private void forget(Mannequin mannequin) {
        queue.cancel(mannequin.id());
        try {
            wardrobes.delete(mannequin.id());
            mannequins.delete(mannequin.id());
        } catch (IOException failed) {
            logger.log(Level.SEVERE, "Mannequin " + mannequin.id() + " broke and dropped its items, but its files"
                    + " could not be deleted. Delete them by hand so its chunk doesn't keep a stale record.", failed);
        }
    }
}
