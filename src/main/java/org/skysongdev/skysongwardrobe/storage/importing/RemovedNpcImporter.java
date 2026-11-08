package org.skysongdev.skysongwardrobe.storage.importing;

import org.skysongdev.skysongwardrobe.npc.RemovedNpc;
import org.skysongdev.skysongwardrobe.npc.RemovedNpcStore;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Copies who each removed-NPC entry was into the database; an entry it already has is skipped. */
public final class RemovedNpcImporter implements DatasetImporter {
    private final RemovedNpcStore source;
    private final RemovedNpcStore target;

    public RemovedNpcImporter(RemovedNpcStore source, RemovedNpcStore target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public String dataset() {
        return "removed npcs";
    }

    @Override
    public ImportResult run() throws IOException {
        Set<UUID> present = target.readAll().stream().map(RemovedNpc::entry).collect(Collectors.toSet());
        int imported = 0;
        int skipped = 0;
        for (RemovedNpc entry : source.readAll()) {
            if (present.contains(entry.entry())) {
                skipped++;
            } else {
                target.write(entry);
                imported++;
            }
        }
        return new ImportResult(dataset(), imported, skipped);
    }
}
