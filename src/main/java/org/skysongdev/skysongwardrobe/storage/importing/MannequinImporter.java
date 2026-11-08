package org.skysongdev.skysongwardrobe.storage.importing;

import org.skysongdev.skysongwardrobe.mannequin.MannequinStore;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Copies the mannequin record files into the database; a mannequin it already has is skipped. */
public final class MannequinImporter implements DatasetImporter {
    private final MannequinStore files;
    private final MannequinStore target;

    public MannequinImporter(MannequinStore files, MannequinStore target) {
        this.files = files;
        this.target = target;
    }

    @Override
    public String dataset() {
        return "mannequins";
    }

    @Override
    public ImportResult run() throws IOException {
        MannequinStore.Contents existing = target.readAll();
        Set<UUID> known = existing.rows().stream().map(MannequinStore.Row::id).collect(Collectors.toSet());
        known.addAll(existing.unreadable());
        int imported = 0;
        int skipped = 0;
        for (MannequinStore.Row row : files.readAll().rows()) {
            if (known.contains(row.id())) {
                skipped++;
            } else {
                target.write(row);
                imported++;
            }
        }
        return new ImportResult(dataset(), imported, skipped);
    }
}
