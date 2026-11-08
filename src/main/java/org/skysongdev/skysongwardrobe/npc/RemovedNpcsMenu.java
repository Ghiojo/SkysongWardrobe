package org.skysongdev.skysongwardrobe.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.config.GalleryMenuLayout;
import org.skysongdev.skysongwardrobe.config.MenuItem;
import org.skysongdev.skysongwardrobe.config.Messages;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.menu.ClickableMenu;
import org.skysongdev.skysongwardrobe.menu.GalleryPages;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * {@code /sw npc removed}: the removed NPCs that still have items, newest first, 45 a page with
 * the gallery's arrows. Clicking one opens its archived wardrobe, which items only come out of.
 */
public final class RemovedNpcsMenu implements ClickableMenu {
    static final int SIZE = 54;
    static final int PREVIOUS_SLOT = 45;
    static final int NEXT_SLOT = 53;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final List<RemovedNpc> entries;
    private final NpcArchive archive;
    private final GalleryMenuLayout arrows;
    private final WardrobeLayout wardrobe;
    private final ItemTagGateway tags;
    private final Messages messages;
    private final Inventory inventory;
    private int page;

    public RemovedNpcsMenu(NpcArchive archive, GalleryMenuLayout arrows, WardrobeLayout wardrobe, ItemTagGateway tags,
                           Messages messages) {
        this.entries = archive.list();
        this.archive = archive;
        this.arrows = arrows;
        this.wardrobe = wardrobe;
        this.tags = tags;
        this.messages = messages;
        this.inventory = Bukkit.createInventory(this, SIZE, messages.get("npc.removed.title"));
        draw();
    }

    /** Whether any removed NPC still has items. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public void click(int slot, Player viewer) {
        if (slot == PREVIOUS_SLOT && page > 0) {
            page--;
            draw();
        } else if (slot == NEXT_SLOT && page < GalleryPages.count(entries.size()) - 1) {
            page++;
            draw();
        } else if (slot < GalleryPages.PER_PAGE && slot < GalleryPages.page(entries, page).size()) {
            RemovedNpc entry = GalleryPages.page(entries, page).get(slot);
            viewer.openInventory(new WardrobeMenu(WardrobeKind.REMOVED_NPC, archive.wardrobes().load(entry.entry()),
                    wardrobe, tags).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void draw() {
        inventory.clear();
        List<RemovedNpc> shown = GalleryPages.page(entries, page);
        for (int slot = 0; slot < shown.size(); slot++) {
            inventory.setItem(slot, icon(shown.get(slot)));
        }
        for (int slot = GalleryPages.PER_PAGE; slot < SIZE; slot++) {
            inventory.setItem(slot, item(arrows.filler()));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, item(arrows.previous()));
        }
        if (page < GalleryPages.count(entries.size()) - 1) {
            inventory.setItem(NEXT_SLOT, item(arrows.next()));
        }
    }

    private ItemStack icon(RemovedNpc entry) {
        Map<String, String> placeholders = Map.of("name", entry.name(), "number", String.valueOf(entry.number()),
                "date", DATE.format(entry.removedAt()));
        ItemStack icon = new ItemStack(Material.ARMOR_STAND);
        icon.editMeta(meta -> {
            meta.displayName(plain(messages.get("npc.removed.entry-name", placeholders)));
            meta.lore(List.of(plain(messages.get("npc.removed.entry-lore", placeholders))));
        });
        return icon;
    }

    private static Component plain(Component text) {
        return text.decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(MenuItem item) {
        ItemStack stack = new ItemStack(item.material());
        stack.editMeta(meta -> meta.displayName(plain(MiniMessage.miniMessage().deserialize(item.name()))));
        return stack;
    }
}
