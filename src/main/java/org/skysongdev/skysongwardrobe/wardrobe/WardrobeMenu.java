package org.skysongdev.skysongwardrobe.wardrobe;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.MannequinEquipment;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * One open wardrobe menu: filler everywhere but the slots, which show the stored items. A kind
 * with equipment, such as a mannequin, also has the locked {@link MannequinEquipment} slots,
 * which take armour and held items instead of wardrobe items.
 * Every move goes through here, so the slot rules hold whether a player clicks, shift-clicks
 * or uses the cursor; {@link WardrobeMenuListener} turns inventory events into these calls.
 */
public final class WardrobeMenu implements InventoryHolder {
    /** Why a move was refused, each with its own message. */
    public enum Refusal { NO_TYPE, NOT_ACCEPTED, SLOT_FULL, NO_FREE_SLOT, TAKE_OUT_ONLY }

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
    private final WardrobeKind kind;
    private final boolean equipment;
    private final Inventory inventory;
    private boolean changed;

    /** A player's wardrobe menu. */
    public WardrobeMenu(Wardrobe wardrobe, WardrobeLayout layout, ItemTagGateway tags) {
        this(WardrobeKind.PLAYER, wardrobe, layout, tags);
    }

    /** A menu on a wardrobe of the given kind. */
    public WardrobeMenu(WardrobeKind kind, Wardrobe wardrobe, WardrobeLayout layout, ItemTagGateway tags) {
        this.kind = kind;
        this.wardrobe = wardrobe;
        this.layout = layout;
        this.tags = tags;
        this.equipment = kind.hasEquipment();
        this.inventory = Bukkit.createInventory(this, WardrobeLayout.SIZE, MiniMessage.miniMessage().deserialize(layout.title()));
        draw();
    }

    /** A mannequin's wardrobe menu, with its equipment slots. */
    public static WardrobeMenu forMannequin(Wardrobe wardrobe, WardrobeLayout layout, ItemTagGateway tags) {
        return new WardrobeMenu(WardrobeKind.MANNEQUIN, wardrobe, layout, tags);
    }

    /** The wardrobe menu a player is looking at, if any. */
    public static Optional<WardrobeMenu> of(InventoryView view) {
        Inventory top = view == null ? null : view.getTopInventory();
        return top != null && top.getHolder(false) instanceof WardrobeMenu menu ? Optional.of(menu) : Optional.empty();
    }

    public Wardrobe wardrobe() {
        return wardrobe;
    }

    /** Whose wardrobe this menu shows. */
    public WardrobeKind kind() {
        return kind;
    }

    /** Whether this menu shows the wardrobe of {@code wearer}, a wearer of the given kind. */
    public boolean shows(WardrobeKind kind, UUID wearer) {
        return this.kind == kind && wardrobe.owner().equals(wearer);
    }

    /** Whether any item went in or out since the menu opened. */
    public boolean changed() {
        return changed;
    }

    /** Puts one of {@code item} into the slot at {@code position}; returns the rest of the stack. */
    public Move placeInto(int position, ItemStack item) {
        if (!kind.acceptsItems() && item != null && !item.isEmpty()) {
            return Move.refused(item, Refusal.TAKE_OUT_ONLY);
        }
        Optional<MannequinEquipment> worn = equipmentAt(position);
        if (worn.isPresent() && item != null && !item.isEmpty()) {
            return placeEquipment(worn.get(), item);
        }
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
        return Move.done(store(slot.get().id(), item));
    }

    /** Puts one of {@code item} into the first free slot that accepts it, for shift-clicks. */
    public Move placeAnywhere(ItemStack item) {
        if (!kind.acceptsItems()) {
            return Move.refused(item, Refusal.TAKE_OUT_ONLY);
        }
        Optional<String> type = tags.type(item);
        Optional<SlotDefinition> free = type.flatMap(wanted -> layout.slots().stream()
                .filter(slot -> slot.accepts(wanted) && wardrobe.item(slot.id()).isEmpty())
                .findFirst());
        if (free.isPresent()) {
            return Move.done(store(free.get().id(), item));
        }
        Optional<MannequinEquipment> freeEquipment = equipment
                ? Arrays.stream(MannequinEquipment.values())
                        .filter(slot -> slot.fits(item) && wardrobe.item(slot.wardrobeId()).isEmpty())
                        .findFirst()
                : Optional.empty();
        if (freeEquipment.isPresent()) {
            return Move.done(store(freeEquipment.get().wardrobeId(), item));
        }
        return Move.refused(item, type.isEmpty() && !equipment ? Refusal.NO_TYPE : Refusal.NO_FREE_SLOT);
    }

    /** Takes the item out of the slot at {@code position}; empty if there is none. */
    public Optional<ItemStack> takeOut(int position) {
        Optional<String> id = storedAt(position);
        Optional<ItemStack> item = id.flatMap(wardrobe::item);
        item.ifPresent(taken -> {
            wardrobe.put(id.get(), null);
            changed = true;
            draw();
        });
        return item;
    }

    /** Puts an item back after it could not be given to the player. */
    public void undoTakeOut(int position, ItemStack item) {
        storedAt(position).ifPresent(id -> {
            wardrobe.put(id, item);
            draw();
        });
    }

    /** Shows the wardrobe's current items again, after another menu on the same wardrobe changed it. */
    public void draw() {
        ItemStack filler = filler();
        for (int position = 0; position < WardrobeLayout.SIZE; position++) {
            Optional<String> id = storedAt(position);
            inventory.setItem(position, id.isPresent() ? wardrobe.item(id.get()).orElse(null) : filler);
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

    private Move placeEquipment(MannequinEquipment slot, ItemStack item) {
        if (wardrobe.item(slot.wardrobeId()).isPresent()) {
            return Move.refused(item, Refusal.SLOT_FULL);
        }
        if (!slot.fits(item)) {
            return Move.refused(item, Refusal.NOT_ACCEPTED);
        }
        return Move.done(store(slot.wardrobeId(), item));
    }

    private Optional<MannequinEquipment> equipmentAt(int position) {
        return equipment ? MannequinEquipment.at(position) : Optional.empty();
    }

    /** The wardrobe key of whatever is stored at a menu position: a slot id or an equipment key. */
    private Optional<String> storedAt(int position) {
        Optional<MannequinEquipment> worn = equipmentAt(position);
        if (worn.isPresent()) {
            return Optional.of(worn.get().wardrobeId());
        }
        return layout.slotAt(position).map(SlotDefinition::id);
    }

    private ItemStack store(String id, ItemStack item) {
        wardrobe.put(id, item.asOne());
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
