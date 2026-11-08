package org.skysongdev.skysongwardrobe.storage.importing;

/**
 * What one {@link DatasetImporter} did.
 *
 * @param imported entries copied into the database
 * @param skipped  entries the database already had, left as they were
 */
public record ImportResult(String dataset, int imported, int skipped) {
}
