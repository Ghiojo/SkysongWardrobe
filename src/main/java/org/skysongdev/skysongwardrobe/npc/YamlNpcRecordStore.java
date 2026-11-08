package org.skysongdev.skysongwardrobe.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves NPC records to {@code <folder>/<uuid>.yml}. A file that can't be read is left untouched
 * and treated as missing, so nothing in it is lost.
 */
public final class YamlNpcRecordStore implements NpcRecordStore {
    private static final String VALUE = "base.value";
    private static final String SIGNATURE = "base.signature";

    private final Path folder;
    private final Logger logger;

    public YamlNpcRecordStore(Path folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    @Override
    public Optional<NpcRecord> read(UUID id) {
        Path path = file(id);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        YamlConfiguration file = YamlConfiguration.loadConfiguration(path.toFile());
        String value = file.getString(VALUE);
        String signature = file.getString(SIGNATURE);
        if (value == null || signature == null) {
            logger.warning(path + " has no base skin, so it is kept untouched and the NPC is treated as undressed.");
            return Optional.empty();
        }
        return Optional.of(new NpcRecord(id, new SignedTexture(value, signature)));
    }

    @Override
    public void write(NpcRecord record) throws IOException {
        YamlConfiguration file = new YamlConfiguration();
        file.set(VALUE, record.base().value());
        file.set(SIGNATURE, record.base().signature());
        Files.createDirectories(folder);
        file.save(file(record.id()).toFile());
    }

    @Override
    public void delete(UUID id) throws IOException {
        Files.deleteIfExists(file(id));
    }

    /** Where the NPC's record is saved. */
    public Path file(UUID id) {
        return folder.resolve(id + ".yml");
    }
}
