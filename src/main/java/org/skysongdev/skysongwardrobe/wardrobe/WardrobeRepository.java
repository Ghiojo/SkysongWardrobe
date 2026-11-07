package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves wardrobes to {@code wardrobes/<uuid>.yml}, each item as Paper's serialized item bytes
 * in Base64. Keyed by UUID, so a name change keeps the wardrobe. An item that can't be read
 * (say, after a downgrade) is kept as it was in the file, so saving never loses it.
 */
public final class WardrobeRepository {
    private static final String SLOTS = "slots";

    private final Path folder;
    private final Logger logger;
    private final Map<UUID, Wardrobe> loaded = new HashMap<>();
    private final Map<UUID, Map<String, String>> unreadable = new HashMap<>();

    public WardrobeRepository(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    /** The owner's wardrobe, read from disk the first time; empty if they have none yet. */
    public synchronized Wardrobe load(UUID owner) {
        return loaded.computeIfAbsent(owner, this::read);
    }

    public synchronized void save(Wardrobe wardrobe) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set("owner", wardrobe.owner().toString());
        unreadable.getOrDefault(wardrobe.owner(), Map.of()).forEach((slot, raw) -> file.set(SLOTS + "." + slot, raw));
        wardrobe.items().forEach((slot, item) ->
                file.set(SLOTS + "." + slot, Base64.getEncoder().encodeToString(item.serializeAsBytes())));
        Files.createDirectories(folder);
        file.save(file(wardrobe.owner()).toFile());
    }

    /** Forgets the cached copy, for an owner who left; the next load reads the file again. */
    public synchronized void unload(UUID owner) {
        loaded.remove(owner);
        unreadable.remove(owner);
    }

    private Wardrobe read(UUID owner) {
        Wardrobe wardrobe = new Wardrobe(owner);
        Path path = file(owner);
        if (!Files.isRegularFile(path)) {
            return wardrobe;
        }
        ConfigurationSection slots = YamlConfiguration.loadConfiguration(path.toFile()).getConfigurationSection(SLOTS);
        if (slots == null) {
            return wardrobe;
        }
        for (String slot : slots.getKeys(false)) {
            String raw = slots.getString(slot);
            try {
                wardrobe.put(slot, ItemStack.deserializeBytes(Base64.getDecoder().decode(raw)));
            } catch (RuntimeException broken) {
                logger.warning(path + ": the item in " + slot + " can't be read, so it is kept in the file untouched.");
                unreadable.computeIfAbsent(owner, key -> new HashMap<>()).put(slot, raw);
            }
        }
        return wardrobe;
    }

    private Path file(UUID owner) {
        return folder.resolve(owner + ".yml");
    }
}
