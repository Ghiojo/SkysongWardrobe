package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongwardrobe.api.NpcAccessRule;
import org.skysongdev.skysongwardrobe.api.NpcView;
import org.skysongdev.skysongwardrobe.api.Npcs;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Npcs} on the Citizens hook. Rules other plugins register join the plugin's own
 * {@code NpcAccess}, wrapped so they see an {@link NpcView}, and leave it when their plugin disables.
 */
final class NpcsImpl implements Npcs, Listener {
    private final MainThread mainThread;
    private final Supplier<Optional<NpcHooks>> hooks;
    private final Map<Plugin, List<org.skysongdev.skysongwardrobe.npc.NpcAccessRule>> rules = new HashMap<>();

    NpcsImpl(MainThread mainThread, Supplier<Optional<NpcHooks>> hooks) {
        this.mainThread = mainThread;
        this.hooks = hooks;
    }

    @Override
    public boolean available() {
        mainThread.check();
        return hooks.get().isPresent();
    }

    @Override
    public Optional<NpcView> find(int number) {
        mainThread.check();
        return lookUp(hooks -> hooks.directory().byNumber(number)).map(NpcsImpl::view);
    }

    @Override
    public Optional<NpcView> find(UUID id) {
        mainThread.check();
        Objects.requireNonNull(id, "id");
        return lookUp(hooks -> hooks.directory().byId(id)).map(NpcsImpl::view);
    }

    @Override
    public boolean canOpenWardrobe(Player viewer, int npcNumber) {
        mainThread.check();
        return mayOpen(viewer, hooks -> hooks.directory().byNumber(npcNumber));
    }

    @Override
    public boolean canOpenWardrobe(Player viewer, UUID npcId) {
        mainThread.check();
        Objects.requireNonNull(npcId, "npcId");
        return mayOpen(viewer, hooks -> hooks.directory().byId(npcId));
    }

    @Override
    public void registerAccessRule(Plugin plugin, NpcAccessRule rule) {
        mainThread.check();
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(rule, "rule");
        org.skysongdev.skysongwardrobe.npc.NpcAccessRule wrapped = (viewer, npc) -> rule.allows(viewer, view(npc));
        rules.computeIfAbsent(plugin, owner -> new ArrayList<>()).add(wrapped);
        hooks.get().ifPresent(found -> found.access().register(wrapped));
    }

    @Override
    public void unregisterAccessRules(Plugin plugin) {
        mainThread.check();
        List<org.skysongdev.skysongwardrobe.npc.NpcAccessRule> removed = rules.remove(Objects.requireNonNull(plugin, "plugin"));
        if (removed != null) {
            hooks.get().ifPresent(found -> removed.forEach(found.access()::unregister));
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        unregisterAccessRules(event.getPlugin());
    }

    private Optional<NpcInfo> lookUp(Function<NpcHooks, Optional<NpcInfo>> find) {
        return hooks.get().flatMap(find);
    }

    private boolean mayOpen(Player viewer, Function<NpcHooks, Optional<NpcInfo>> find) {
        Objects.requireNonNull(viewer, "viewer");
        Optional<NpcHooks> found = hooks.get();
        return found.isPresent() && find.apply(found.get()).map(npc -> found.get().opener().mayOpen(viewer, npc))
                .orElse(false);
    }

    static NpcView view(NpcInfo npc) {
        return new NpcView(npc.id(), npc.number(), npc.name(), npc.owner());
    }
}
