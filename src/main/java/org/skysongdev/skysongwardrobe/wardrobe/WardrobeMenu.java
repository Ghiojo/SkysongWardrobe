package org.skysongdev.skysongwardrobe.wardrobe;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;

import java.util.Optional;

/**
 * One open wardrobe menu: filler everywhere but the slots, which show the stored items.
 * Every move goes through here, so the slot rules hold whether a player clicks, shift-clicks
 * or uses the cursor; {@link WardrobeMenuListener} turns inventory events into these calls.
 */
public final class WardrobeMenu implements InventoryHolder {
    /** Why a move was refused, each with its own message. */
    public enum Refusal { NO_TYPE, NOT_ACCEPTED, SLOT_FULL, NO_FREE_SLOT }

    /** The outcome of a move: what is left over, and why it was refused if it was. */
    public record Move(ItemStack rest, Optional<Refusal> refusal) {
        static Move done(ItemStack rest) {
            return new Move(rest, Optional.empty());
        }

        static Move refused(ItemStack rest, Refusal why) {
            return new Move(rest, Optional.of(why));
        }
    }

    private final Wardrobe wardrobe;
    private final WardrobeLayout layout;
    private final ItemTagGateway tags;
    private final Inventory inventory;
    private boolean changed;

    public WardrobeMenu(Wardrobe wardrobe, WardrobeLayout layout, ItemTagGateway tags) {
        this.wardrobe = wardrobe;
        this.layout = layout;
        this.tags = tags;
        this.inventory = Bukkit.createInventory(this, WardrobeLayout.SIZE, MiniMessage.miniMessage().deserialize(layout.title()));
        draw();
    }

    public Wardrobe wardrobe() {
        return wardrobe;
    }

    /** Whether any item went in or out since the menu opened. */
    public boolean changed() {
        return changed;
    }

    /** Puts one of {@code item} into the slot at {@code position}; returns the rest of the stack. */
    public Move placeInto(int position, ItemStack item) {
        Optional<SlotDefinition> slot = layout.slotAt(position);
        if (slot.isEmpty() || item == null || item.isEmpty()) {
            return Move.done(item);
        }
        if (wardrobe.item(slot.get().id()).isPresent()) {
            return Move.refused(item, Refusal.SLOT_FULL);
        }
        Optional<Refusal> refusal = check(slot.get(), item);
        if (refusal.isPresent()) {
            return Move.refused(item, refusal.get());
        }
        return Move.done(store(slot.get(), item));
    }

    /** Puts one of {@code item} into the first free slot that accepts it, for shift-clicks. */
    public Move placeAnywhere(ItemStack item) {
        Optional<String> type = tags.type(item);
        if (type.isEmpty()) {
            return Move.refused(item, Refusal.NO_TYPE);
        }
        Optional<SlotDefinition> free = layout.slots().stream()
                .filter(slot -> slot.accepts(type.get()) && wardrobe.item(slot.id()).isEmpty())
                .findFirst();
        return free.map(slot -> Move.done(store(slot, item))).orElseGet(() -> Move.refused(item, Refusal.NO_FREE_SLOT));
    }

    /** Takes the item out of the slot at {@code position}; empty if there is none. */
    public Optional<ItemStack> takeOut(int position) {
        Optional<SlotDefinition> slot = layout.slotAt(position);
        Optional<ItemStack> item = slot.flatMap(definition -> wardrobe.item(definition.id()));
        item.ifPresent(taken -> {
            wardrobe.put(slot.get().id(), null);
            changed = true;
            draw();
        });
        return item;
    }

    /** Puts an item back after it could not be given to the player. */
    public void undoTakeOut(int position, ItemStack item) {
        layout.slotAt(position).ifPresent(slot -> {
            wardrobe.put(slot.id(), item);
            draw();
        });
    }

    /** Shows the wardrobe's current items again, after another menu on the same wardrobe changed it. */
    public void draw() {
        ItemStack filler = filler();
        for (int position = 0; position < WardrobeLayout.SIZE; position++) {
            Optional<SlotDefinition> slot = layout.slotAt(position);
            inventory.setItem(position, slot.isPresent() ? wardrobe.item(slot.get().id()).orElse(null) : filler);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private Optional<Refusal> check(SlotDefinition slot, ItemStack item) {
        Optional<String> type = tags.type(item);
        if (type.isEmpty()) {
            return Optional.of(Refusal.NO_TYPE);
        }
        return slot.accepts(type.get()) ? Optional.empty() : Optional.of(Refusal.NOT_ACCEPTED);
    }

    private ItemStack store(SlotDefinition slot, ItemStack item) {
        wardrobe.put(slot.id(), item.asOne());
        changed = true;
        draw();
        return item.getAmount() > 1 ? item.asQuantity(item.getAmount() - 1) : ItemStack.empty();
    }

    private ItemStack filler() {
        ItemStack filler = new ItemStack(layout.fillerMaterial());
        filler.editMeta(meta -> meta.displayName(MiniMessage.miniMessage().deserialize(layout.fillerName())
                .decoration(TextDecoration.ITALIC, false)));
        return filler;
    }
}
