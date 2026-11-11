package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Changes a wardrobe without a menu, for other plugins: the menu's slot rules, the same save,
 * open menus on that wardrobe redrawn, and the kind's change handler run so the wearer renders
 * again. Main thread only.
 */
public final class WardrobeEditor {
    /** How a change went. */
    public enum Outcome { CHANGED, UNKNOWN_SLOT, NOT_WEARABLE, WRONG_SLOT, WARDROBE_CLOSED }

    private final WardrobeKinds kinds;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Server server;
    private final Logger logger;

    /** @param tags looked up per change, since SkysongItems may not be ready at startup */
    public WardrobeEditor(WardrobeKinds kinds, Supplier<ItemTagGateway> tags, PluginConfiguration configuration,
                          Server server, Logger logger) {
        this.kinds = kinds;
        this.tags = tags;
        this.configuration = configuration;
        this.server = server;
        this.logger = logger;
    }

    /** Every item in the wearer's wardrobe, by slot id, as copies. */
    public Map<String, ItemStack> items(WardrobeKind kind, UUID wearer) {
        return stored(kind).wardrobes().load(wearer).items();
    }

    /**
     * Puts one of {@code item} into the slot with this id, replacing what was there; a null or
     * empty item clears it.
     */
    public Outcome set(WardrobeKind kind, UUID wearer, String slotId, ItemStack item) {
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty()) {
            return Outcome.WARDROBE_CLOSED;
        }
        Optional<SlotDefinition> slot = layout.get().slots().stream().filter(each -> each.id().equals(slotId)).findFirst();
        if (slot.isEmpty()) {
            return Outcome.UNKNOWN_SLOT;
        }
        boolean clearing = item == null || item.isEmpty();
        if (!clearing) {
            Optional<String> type = tags.get().type(item);
            if (type.isEmpty()) {
                return Outcome.NOT_WEARABLE;
            }
            if (!slot.get().accepts(type.get())) {
                return Outcome.WRONG_SLOT;
            }
        }
        WardrobeKinds.Kind stored = stored(kind);
        Wardrobe wardrobe = stored.wardrobes().load(wearer);
        wardrobe.put(slotId, clearing ? null : item.asOne());
        stored.wardrobes().save(wardrobe).exceptionally(failed -> {
            logger.log(Level.WARNING, "Could not save the " + kind + " wardrobe of " + wearer + ".", failed);
            return null;
        });
        redraw(kind, wearer);
        stored.changed().accept(wearer, null);
        return Outcome.CHANGED;
    }

    private WardrobeKinds.Kind stored(WardrobeKind kind) {
        return kinds.get(kind).orElseThrow(() -> new IllegalStateException("Nothing stores " + kind + " wardrobes."));
    }

    private void redraw(WardrobeKind kind, UUID wearer) {
        for (Player online : server.getOnlinePlayers()) {
            WardrobeMenu.of(online.getOpenInventory()).filter(menu -> menu.shows(kind, wearer)).ifPresent(WardrobeMenu::draw);
        }
    }
}
