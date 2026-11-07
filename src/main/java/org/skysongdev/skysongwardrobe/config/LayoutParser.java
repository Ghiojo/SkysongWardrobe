package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns wardrobe.yml into a {@link WardrobeLayout}. Unlike config.yml, a broken layout is not
 * patched up: two slots on one position, or two slots with the same priority, have no sensible
 * fallback. The parse then gives no layout and {@link #errors()} says why, so the menu stays
 * closed until the file is fixed.
 */
public final class LayoutParser {
    static final String DEFAULT_TITLE = "<dark_gray>Wardrobe";
    static final Material DEFAULT_FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private final List<String> errors = new ArrayList<>();

    /** The layout, or empty when wardrobe.yml has any error. */
    public Optional<WardrobeLayout> parse(ConfigurationSection config) {
        errors.clear();
        String title = config.getString("title", DEFAULT_TITLE);
        Material filler = filler(config.getString("filler.material"));
        String fillerName = config.getString("filler.name", " ");
        List<SlotDefinition> slots = slots(config.getConfigurationSection("slots"));
        if (!errors.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WardrobeLayout(title, filler, fillerName, slots));
    }

    /** Why the last {@link #parse} gave no layout, one line each; empty when it gave one. */
    public List<String> errors() {
        return List.copyOf(errors);
    }

    private Material filler(String name) {
        if (name == null) {
            return DEFAULT_FILLER;
        }
        Material material = Material.matchMaterial(name);
        if (material == null || !material.isItem() || material.isAir()) {
            errors.add("filler.material " + name + " is not an item.");
            return DEFAULT_FILLER;
        }
        return material;
    }

    private List<SlotDefinition> slots(ConfigurationSection section) {
        List<SlotDefinition> slots = new ArrayList<>();
        if (section == null || section.getKeys(false).isEmpty()) {
            errors.add("slots lists no slots.");
            return slots;
        }
        Map<Integer, String> positions = new HashMap<>();
        Map<Integer, String> priorities = new HashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection slot = section.getConfigurationSection(id);
            if (slot == null) {
                errors.add("slots." + id + " is not a section.");
                continue;
            }
            int position = slot.getInt("position", -1);
            if (!slot.isInt("position") || position < 0 || position >= WardrobeLayout.SIZE) {
                errors.add("slots." + id + ".position must be a whole number from 0 to 53.");
            } else if (positions.containsKey(position)) {
                errors.add("slots." + id + " and slots." + positions.get(position) + " share position " + position + ".");
            } else {
                positions.put(position, id);
            }

            int priority = slot.getInt("priority");
            if (!slot.isInt("priority")) {
                errors.add("slots." + id + ".priority must be a whole number.");
            } else if (priorities.containsKey(priority)) {
                errors.add("slots." + id + " and slots." + priorities.get(priority)
                        + " share priority " + priority + ", so their draw order is undefined.");
            } else {
                priorities.put(priority, id);
            }

            Set<String> accepts = accepts(slot.getStringList("accepts"));
            if (accepts.isEmpty()) {
                errors.add("slots." + id + ".accepts lists no wardrobe_type values.");
            }
            slots.add(new SlotDefinition(id, position, accepts, priority));
        }
        return slots;
    }

    private static Set<String> accepts(List<String> values) {
        Set<String> accepts = new HashSet<>();
        for (String value : values) {
            if (!value.isBlank()) {
                accepts.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return accepts;
    }
}
