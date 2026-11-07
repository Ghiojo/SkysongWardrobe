package org.skysongdev.skysongwardrobe.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.GalleryMenuLayout;
import org.skysongdev.skysongwardrobe.config.MenuItem;
import org.skysongdev.skysongwardrobe.config.Messages;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;

import java.util.List;
import java.util.Map;

/**
 * One open gallery menu: entries as their icons in id order, 45 a page, with page arrows on
 * the bottom row. Nothing in it can be taken; {@link MenuListener} cancels every click.
 */
public final class GalleryMenu implements InventoryHolder {
    static final int SIZE = 54;
    static final int PREVIOUS_SLOT = 45;
    static final int NEXT_SLOT = 53;
    static final Material DEFAULT_ICON = Material.PAPER;

    private final List<SkinEntry> entries;
    private final GalleryMenuLayout layout;
    private final Messages messages;
    private final Inventory inventory;
    private int page;

    public GalleryMenu(String ownerName, List<SkinEntry> entries, GalleryMenuLayout layout, Messages messages) {
        this.entries = List.copyOf(entries);
        this.layout = layout;
        this.messages = messages;
        Component title = MiniMessage.miniMessage().deserialize(layout.title(), Placeholder.unparsed("player", ownerName));
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        draw();
    }

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    /** Turns the page when an arrow is clicked; every other position does nothing. */
    void click(int slot) {
        if (slot == PREVIOUS_SLOT && page > 0) {
            page--;
            draw();
        } else if (slot == NEXT_SLOT && page < GalleryPages.count(entries.size()) - 1) {
            page++;
            draw();
        }
    }

    int page() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void draw() {
        inventory.clear();
        List<SkinEntry> shown = GalleryPages.page(entries, page);
        for (int slot = 0; slot < shown.size(); slot++) {
            inventory.setItem(slot, icon(shown.get(slot)));
        }
        for (int slot = GalleryPages.PER_PAGE; slot < SIZE; slot++) {
            inventory.setItem(slot, item(layout.filler()));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, item(layout.previous()));
        }
        if (page < GalleryPages.count(entries.size()) - 1) {
            inventory.setItem(NEXT_SLOT, item(layout.next()));
        }
    }

    private ItemStack icon(SkinEntry entry) {
        ItemStack icon = new ItemStack(entry.icon().orElse(DEFAULT_ICON));
        icon.editMeta(meta -> {
            meta.displayName(Component.text(entry.name()).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(messages.get("gallery.entry-lore", Map.of("id", entry.id().value()))
                    .decoration(TextDecoration.ITALIC, false)));
        });
        return icon;
    }

    private static ItemStack item(MenuItem item) {
        ItemStack stack = new ItemStack(item.material());
        stack.editMeta(meta -> meta.displayName(MiniMessage.miniMessage().deserialize(item.name())
                .decoration(TextDecoration.ITALIC, false)));
        return stack;
    }
}
