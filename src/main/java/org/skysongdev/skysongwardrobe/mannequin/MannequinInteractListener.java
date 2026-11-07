package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Right-clicking a mannequin opens its wardrobe, for whoever {@link MannequinAccess} allows.
 * Only one person may have a mannequin's wardrobe open at a time, so two can't take the same item.
 */
public final class MannequinInteractListener implements Listener {
    private final MannequinRepository mannequins;
    private final WardrobeRepository wardrobes;
    private final MannequinMarker marker;
    private final MannequinAccess access;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Server server;

    public MannequinInteractListener(MannequinRepository mannequins, WardrobeRepository wardrobes, MannequinMarker marker,
                                     MannequinAccess access, Supplier<ItemTagGateway> tags,
                                     PluginConfiguration configuration, Server server) {
        this.mannequins = mannequins;
        this.wardrobes = wardrobes;
        this.marker = marker;
        this.access = access;
        this.tags = tags;
        this.configuration = configuration;
        this.server = server;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!marker.isMarked(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        Optional<Mannequin> mannequin = mannequins.find(event.getRightClicked().getUniqueId());
        if (event.getHand() != EquipmentSlot.HAND || mannequin.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<WardrobeLayout> layout = configuration.layout();
        if (!access.canUse(player, mannequin.get())) {
            player.sendMessage(configuration.messages().get("mannequin.no-access"));
        } else if (layout.isEmpty()) {
            player.sendMessage(configuration.messages().get("wardrobe.closed"));
        } else if (inUse(mannequin.get().id())) {
            player.sendMessage(configuration.messages().get("mannequin.in-use"));
        } else {
            WardrobeMenu menu = WardrobeMenu.forMannequin(wardrobes.load(mannequin.get().id()), layout.get(), tags.get());
            player.openInventory(menu.getInventory());
        }
    }

    private boolean inUse(UUID id) {
        return server.getOnlinePlayers().stream()
                .flatMap(online -> WardrobeMenu.of(online.getOpenInventory()).stream())
                .anyMatch(menu -> menu.isMannequin() && menu.wardrobe().owner().equals(id));
    }
}
