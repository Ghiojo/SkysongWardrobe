package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * One player's gallery folder: {@code gallery.yml}, which holds the owner's UUID and the
 * entries, and one {@code <id>.png} per entry. Removing an entry keeps its image, so items
 * that already carry the layer still render. Where the folder is, and renaming it, is not
 * this class's concern.
 */
public final class PlayerGallery {
    static final String INDEX_FILE = "gallery.yml";
    static final String IMAGE_EXTENSION = ".png";

    private final Path folder;
    private final UUID owner;
    private final Logger logger;
    private final Map<SkinId, SkinEntry> entries = new TreeMap<>(Comparator.comparing(SkinId::value));

    private PlayerGallery(Path folder, UUID owner, Logger logger) {
        this.folder = folder;
        this.owner = owner;
        this.logger = logger;
    }

    /** Reads the gallery in {@code folder}, or starts an empty one if there is none yet. */
    public static PlayerGallery load(Path folder, UUID owner, Logger logger) {
        PlayerGallery gallery = new PlayerGallery(folder, owner, logger);
        Path index = folder.resolve(INDEX_FILE);
        if (Files.isRegularFile(index)) {
            gallery.read(YamlConfiguration.loadConfiguration(index.toFile()));
        }
        return gallery;
    }

    public UUID owner() {
        return owner;
    }

    public Path folder() {
        return folder;
    }

    /** All entries, ordered by id. */
    public synchronized List<SkinEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized Optional<SkinEntry> entry(SkinId id) {
        return Optional.ofNullable(entries.get(id));
    }

    /** Saves a new entry with its image; false, changing nothing, if the id is taken. */
    public synchronized boolean add(SkinEntry entry, byte[] png) throws IOException {
        if (entries.containsKey(entry.id())) {
            return false;
        }
        Files.createDirectories(folder);
        Files.write(imageFile(entry.id()), png);
        entries.put(entry.id(), entry);
        save();
        return true;
    }

    /** Replaces an entry's name, icon or link; false if there is no entry with that id. */
    public synchronized boolean update(SkinEntry entry) throws IOException {
        if (!entries.containsKey(entry.id())) {
            return false;
        }
        entries.put(entry.id(), entry);
        save();
        return true;
    }

    /** Takes the entry out of the gallery but keeps its image file; false if there was none. */
    public synchronized boolean remove(SkinId id) throws IOException {
        if (entries.remove(id) == null) {
            return false;
        }
        save();
        return true;
    }

    /** Where the entry's image is, whether or not the entry is still listed. */
    public Path imageFile(SkinId id) {
        return folder.resolve(id.value() + IMAGE_EXTENSION);
    }

    private void read(YamlConfiguration index) {
        ConfigurationSection section = index.getConfigurationSection("entries");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Optional<SkinEntry> entry = entry(key, section.getConfigurationSection(key));
            entry.ifPresentOrElse(skin -> entries.put(skin.id(), skin),
                    () -> logger.warning(folder.resolve(INDEX_FILE) + ": skipped broken entry " + key + "."));
        }
    }

    private static Optional<SkinEntry> entry(String key, ConfigurationSection section) {
        Optional<SkinId> id = SkinId.parse(key);
        if (id.isEmpty() || section == null || section.getString("link") == null) {
            return Optional.empty();
        }
        try {
            URI link = new URI(section.getString("link"));
            Optional<Material> icon = Optional.ofNullable(section.getString("icon")).map(Material::matchMaterial);
            return Optional.of(new SkinEntry(id.get(), section.getString("name", key), icon, link,
                    uploadedAt(section.getString("uploaded-at"))));
        } catch (URISyntaxException brokenLink) {
            return Optional.empty();
        }
    }

    /** A hand-written entry may have no time; it then sorts as the oldest. */
    private static Instant uploadedAt(String stored) {
        if (stored == null) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(stored);
        } catch (DateTimeParseException broken) {
            return Instant.EPOCH;
        }
    }

    private void save() throws IOException {
        YamlConfiguration index = new YamlConfiguration();
        index.set("uuid", owner.toString());
        for (SkinEntry entry : entries.values()) {
            String path = "entries." + entry.id().value();
            index.set(path + ".name", entry.name());
            entry.icon().ifPresent(icon -> index.set(path + ".icon", icon.name()));
            index.set(path + ".link", entry.link().toString());
            index.set(path + ".uploaded-at", entry.uploadedAt().toString());
        }
        Files.createDirectories(folder);
        index.save(folder.resolve(INDEX_FILE).toFile());
    }
}
