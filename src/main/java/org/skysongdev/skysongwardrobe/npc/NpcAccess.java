package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.commands.Permissions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Who may open an NPC's wardrobe: anyone any registered {@link NpcAccessRule} allows. The
 * standard rules let staff open every NPC and players open the NPCs they own; other plugins
 * add theirs with {@link #register}.
 */
public final class NpcAccess {
    private final List<NpcAccessRule> rules = new CopyOnWriteArrayList<>();

    /** Staff and owners, from the {@code npcs.permissions} list in config.yml. */
    public static NpcAccess standard(Supplier<List<String>> permissions) {
        return new NpcAccess().register(staff(permissions)).register(owner(permissions));
    }

    /** Holding the {@code .other} variant of any NPC permission opens every NPC. */
    public static NpcAccessRule staff(Supplier<List<String>> permissions) {
        return (viewer, npc) -> Permissions.hasAnyOther(viewer, permissions.get());
    }

    /** Holding any NPC permission opens the NPCs Citizens lists the viewer as owner of. */
    public static NpcAccessRule owner(Supplier<List<String>> permissions) {
        return (viewer, npc) -> npc.owner().map(viewer.getUniqueId()::equals).orElse(false)
                && Permissions.hasAny(viewer, permissions.get());
    }

    /** Adds a rule; any one rule allowing a player lets them in. */
    public NpcAccess register(NpcAccessRule rule) {
        rules.add(rule);
        return this;
    }

    public boolean canOpen(Player viewer, NpcInfo npc) {
        return rules.stream().anyMatch(rule -> rule.allows(viewer, npc));
    }
}
