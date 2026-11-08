package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves wardrobes to {@code <folder>/<uuid>.yml}, each item's bytes in Base64. Keyed by UUID, so a
 * name change keeps the wardrobe. A slot whose text isn't even Base64 (say, edited by hand) is
 * kept in the file as it was and never handed out, so saving never loses it.
 */
public final class YamlWardrobeStore implements WardrobeStore {
    private static final String SLOTS = "slots";

    private final Path folder;
    private final Logger logger;
    private final Map<UUID, Map<String, String>> undecodable = new HashMap<>();

    public YamlWardrobeStore(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    @Override
    public synchronized Map<String, byte[]> read(UUID owner) {
        undecodable.remove(owner);
        Map<String, byte[]> slots = new LinkedHashMap<>();
        Path path = file(owner);
        if (!Files.isRegularFile(path)) {
            return slots;
        }
        ConfigurationSection saved = YamlConfiguration.loadConfiguration(path.toFile()).getConfigurationSection(SLOTS);
        if (saved == null) {
            return slots;
        }
        for (String slot : saved.getKeys(false)) {
            String raw = saved.getString(slot, "");
            try {
                slots.put(slot, Base64.getDecoder().decode(raw));
            } catch (IllegalArgumentException notBase64) {
                logger.warning(path + ": the item in " + slot + " can't be read, so it is kept in the file untouched.");
                undecodable.computeIfAbsent(owner, key -> new HashMap<>()).put(slot, raw);
            }
        }
        return slots;
    }

    @Override
    public synchronized void write(UUID owner, Map<String, byte[]> slots) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set("owner", owner.toString());
        undecodable.getOrDefault(owner, Map.of()).forEach((slot, raw) -> file.set(SLOTS + "." + slot, raw));
        slots.forEach((slot, bytes) -> file.set(SLOTS + "." + slot, Base64.getEncoder().encodeToString(bytes)));
        Files.createDirectories(folder);
        file.save(file(owner).toFile());
    }

    /** Whether the owner's file, as last read, has slots whose text isn't Base64. */
    public synchronized boolean keepsUndecodable(UUID owner) {
        return undecodable.containsKey(owner);
    }

    /** Keeping aside renames the file to {@code <uuid>.broken.yml}. */
    @Override
    public synchronized void delete(UUID owner, boolean keepAside) throws IOException {
        Path path = file(owner);
        boolean broken = keepAside || undecodable.containsKey(owner);
        if (broken && Files.isRegularFile(path)) {
            Files.move(path, folder.resolve(owner + ".broken.yml"), StandardCopyOption.REPLACE_EXISTING);
            logger.warning(path + " held items that can't be read, so it was kept as " + owner + ".broken.yml.");
        } else {
            Files.deleteIfExists(path);
        }
        undecodable.remove(owner);
    }

    private Path file(UUID owner) {
        return folder.resolve(owner + ".yml");
    }
}
