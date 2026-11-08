package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.skysongdev.skysongwardrobe.config.Messages;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Right-clicking the top of a block with a mannequin spawner places a mannequin there. A click
 * another plugin (such as a protection plugin) already denied is left alone, and a spawner
 * click is always used up, so the item never does anything else.
 */
public final class MannequinPlaceListener implements Listener {
    private final MannequinPlacer placer;
    private final Supplier<Messages> messages;

    public MannequinPlaceListener(MannequinPlacer placer, Supplier<Messages> messages) {
        this.placer = placer;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getBlockFace() != BlockFace.UP || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        MannequinPlacer.Result result = placer.place(event.getPlayer(), event.getItem(), event.getClickedBlock());
        if (result == MannequinPlacer.Result.NOT_A_SPAWNER) {
            return;
        }
        event.setCancelled(true);
        if (result != MannequinPlacer.Result.PLACED) {
            messages.get().send(event.getPlayer(), "mannequin." + key(result));
        }
    }

    private static String key(MannequinPlacer.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
