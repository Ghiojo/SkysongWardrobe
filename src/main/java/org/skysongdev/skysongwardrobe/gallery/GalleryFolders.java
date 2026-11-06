package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Keeps each gallery folder under {@code skins/} named after its owner's current name, so staff
 * can find them by hand. The owner's UUID in each folder's gallery.yml is the source of truth.
 *
 * <p>When a player joins under a name whose folder belongs to someone else (that player changed
 * name and this one took it), the other folder moves to its owner's current name if Mojang knows
 * it, or to the owner's UUID until they join again.
 */
public final class GalleryFolders {
    private final Path root;
    private final NameLookup names;
    private final Logger logger;
    private final Map<UUID, Path> folders = new HashMap<>();

    public GalleryFolders(Path root, NameLookup names, Logger logger) {
        this.root = root;
        this.names = names;
        this.logger = logger;
    }

    /** Reads which folder belongs to whom. Folders with no readable UUID are left alone. */
    public synchronized void scan() throws IOException {
        folders.clear();
        Files.createDirectories(root);
        try (DirectoryStream<Path> children = Files.newDirectoryStream(root, Files::isDirectory)) {
            for (Path folder : children) {
                Optional<UUID> owner = ownerOf(folder);
                if (owner.isEmpty()) {
                    logger.warning(folder + " has no owner UUID in " + PlayerGallery.INDEX_FILE + ", so it is ignored.");
                } else if (folders.containsKey(owner.get())) {
                    logger.warning(folder + " and " + folders.get(owner.get()) + " belong to the same player; using the second.");
                } else {
                    folders.put(owner.get(), folder);
                }
            }
        }
    }

    /** The owner's folder if they have one, wherever it is named. */
    public synchronized Optional<Path> existing(UUID owner) {
        return Optional.ofNullable(folders.get(owner));
    }

    /**
     * The folder the owner's gallery belongs in, renaming folders so it is {@code skins/<name>}.
     * Blocks on a name lookup when another player's folder has to move, so call it off the main thread.
     * The folder itself is only created once the gallery saves something.
     */
    public synchronized Path claim(UUID owner, String name) throws IOException {
        Path wanted = root.resolve(name);
        Optional<UUID> holder = holderOf(name);
        if (holder.isPresent() && !holder.get().equals(owner)) {
            moveAside(holder.get());
        }
        Path current = folders.get(owner);
        if (current != null && !current.getFileName().toString().equals(name)) {
            move(owner, wanted);
        }
        folders.putIfAbsent(owner, wanted);
        return folders.get(owner);
    }

    private void moveAside(UUID other) throws IOException {
        Optional<String> newName = names.currentName(other).filter(name -> holderOf(name).isEmpty());
        Path target = root.resolve(newName.orElse(other.toString()));
        logger.info("Gallery folder " + folders.get(other).getFileName() + " now belongs to another player, so it moves to "
                + target.getFileName() + ".");
        move(other, target);
    }

    private void move(UUID owner, Path target) throws IOException {
        Path source = folders.get(owner);
        if (Files.exists(source)) {
            // Through a temporary name, so a change of case also works on case-insensitive disks.
            Path temporary = root.resolve(owner + ".moving");
            Files.move(source, temporary);
            Files.move(temporary, target);
        }
        folders.put(owner, target);
    }

    /** Who holds a folder of this name, ignoring case, as Minecraft names and some disks do. */
    private Optional<UUID> holderOf(String name) {
        return folders.entrySet().stream()
                .filter(entry -> entry.getValue().getFileName().toString().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static Optional<UUID> ownerOf(Path folder) {
        Path index = folder.resolve(PlayerGallery.INDEX_FILE);
        if (!Files.isRegularFile(index)) {
            return Optional.empty();
        }
        String uuid = YamlConfiguration.loadConfiguration(index.toFile()).getString("uuid");
        try {
            return Optional.ofNullable(uuid).map(UUID::fromString);
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}
