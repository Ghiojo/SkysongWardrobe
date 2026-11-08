package org.skysongdev.skysongwardrobe.storage.importing;

import org.skysongdev.skysongwardrobe.npc.NpcRecord;
import org.skysongdev.skysongwardrobe.npc.NpcRecordStore;
import org.skysongdev.skysongwardrobe.npc.YamlNpcRecordStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Copies the {@code npcs/<uuid>.yml} record files into the database; an NPC it already has is skipped. */
public final class NpcRecordImporter implements DatasetImporter {
    private static final String SUFFIX = ".yml";

    private final Path folder;
    private final NpcRecordStore target;
    private final Logger logger;

    public NpcRecordImporter(Path folder, NpcRecordStore target, Logger logger) {
        this.folder = folder;
        this.target = target;
        this.logger = logger;
    }

    @Override
    public String dataset() {
        return "npc records";
    }

    @Override
    public ImportResult run() throws IOException {
        YamlNpcRecordStore files = new YamlNpcRecordStore(folder, logger);
        int imported = 0;
        int skipped = 0;
        for (UUID id : ids()) {
            Optional<NpcRecord> record = files.read(id);
            if (record.isEmpty() || target.read(id).isPresent()) {
                skipped++;
            } else {
                target.write(record.get());
                imported++;
            }
        }
        return new ImportResult(dataset(), imported, skipped);
    }

    /** Only the folder's own files: wardrobes/ and removed/ inside it hold other data. */
    private List<UUID> ids() throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(folder)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(SUFFIX))
                    .flatMap(name -> parse(name.substring(0, name.length() - SUFFIX.length())).stream())
                    .toList();
        }
    }

    private static Optional<UUID> parse(String name) {
        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException notAnNpc) {
            return Optional.empty();
        }
    }
}
