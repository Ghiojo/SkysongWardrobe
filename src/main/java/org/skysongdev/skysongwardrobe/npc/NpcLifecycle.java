package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.render.RenderQueue;
import org.skysongdev.skysongwardrobe.wardrobe.Wardrobe;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Follows NPCs Citizens removes or copies. A removed NPC's wardrobe goes to the
 * {@link NpcArchive} so no item is lost; a copy gets the original's wardrobe and base skin, so it
 * renders the same. Main thread only.
 */
public final class NpcLifecycle {
    private final NpcRepository records;
    private final WardrobeRepository wardrobes;
    private final NpcArchive archive;
    private final RenderQueue queue;
    private final Server server;
    private final Clock clock;
    private final Logger logger;

    public NpcLifecycle(NpcRepository records, WardrobeRepository wardrobes, NpcArchive archive, RenderQueue queue,
                        Server server, Clock clock, Logger logger) {
        this.records = records;
        this.wardrobes = wardrobes;
        this.archive = archive;
        this.queue = queue;
        this.server = server;
        this.clock = clock;
        this.logger = logger;
    }

    /** The NPC was removed: close its menus, drop its render, and archive what it wore. */
    public void removed(NpcInfo npc) {
        closeMenus(npc.id());
        queue.cancel(npc.id());
        Wardrobe wardrobe = wardrobes.load(npc.id());
        try {
            if (!wardrobe.items().isEmpty()) {
                logger.info("NPC " + npc.name() + " (" + npc.number() + ") was removed; its wardrobe is kept as removed"
                        + " entry " + archive.store(npc, wardrobe, clock.instant()) + ", see /sw npc removed.");
            }
            wardrobes.delete(npc.id());
            records.delete(npc.id());
        } catch (IOException failed) {
            logger.log(Level.SEVERE, "NPC " + npc.id() + " was removed, but its wardrobe could not be archived."
                    + " Its files are left in place so no item is lost.", failed);
        }
    }

    /** Citizens copied {@code original} as {@code copy}: the copy wears the same outfit on the same base. */
    public void copied(UUID original, UUID copy) {
        Wardrobe from = wardrobes.load(original);
        Wardrobe to = wardrobes.load(copy);
        from.items().forEach(to::put);
        try {
            if (!from.items().isEmpty()) {
                wardrobes.save(to);
            }
            Optional<NpcRecord> record = records.find(original);
            if (record.isPresent()) {
                records.save(record.get().copyFor(copy));
            }
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not copy the wardrobe of NPC " + original + " to its copy " + copy + ".",
                    failed);
        }
    }

    private void closeMenus(UUID npc) {
        for (Player online : server.getOnlinePlayers()) {
            WardrobeMenu.of(online.getOpenInventory())
                    .filter(menu -> menu.shows(WardrobeKind.NPC, npc))
                    .ifPresent(menu -> online.closeInventory());
        }
    }
}
