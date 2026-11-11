package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.npc.NPCSelector;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.npc.NpcDirectory;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Finds NPCs in Citizens' main registry and turns them into {@link NpcInfo}. */
final class CitizensNpcDirectory implements NpcDirectory {
    /** How far away, in blocks, a looked-at NPC may stand. */
    static final int REACH = 5;

    private final Supplier<NPCRegistry> registry;
    private final Supplier<NPCSelector> selector;

    CitizensNpcDirectory(Supplier<NPCRegistry> registry, Supplier<NPCSelector> selector) {
        this.registry = registry;
        this.selector = selector;
    }

    @Override
    public Optional<NpcInfo> byNumber(int number) {
        return info(registry.get().getById(number));
    }

    @Override
    public Optional<NpcInfo> byId(UUID id) {
        return info(registry.get().getByUniqueId(id));
    }

    @Override
    public Optional<NpcInfo> selected(CommandSender sender) {
        return info(selector.get().getSelected(sender));
    }

    @Override
    public Optional<NpcInfo> lookedAt(Player player) {
        Entity target = player.getTargetEntity(REACH);
        return target == null ? Optional.empty() : info(registry.get().getNPC(target));
    }

    /** The plugin's view of an NPC; null gives empty. */
    static Optional<NpcInfo> info(NPC npc) {
        if (npc == null) {
            return Optional.empty();
        }
        Owner owner = npc.getTraitNullable(Owner.class);
        return Optional.of(new NpcInfo(npc.getUniqueId(), npc.getId(), npc.getName(),
                Optional.ofNullable(owner == null ? null : owner.getOwnerId())));
    }
}
