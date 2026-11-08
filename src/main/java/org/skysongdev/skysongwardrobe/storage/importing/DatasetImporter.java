package org.skysongdev.skysongwardrobe.storage.importing;

import java.io.IOException;

/**
 * Copies one kind of data from the YAML files into the database. It only adds what the database
 * doesn't have yet and never changes or deletes a file, so it is safe to run twice, and switching
 * back to {@code storage.type: yaml} finds the files as they were.
 */
public interface DatasetImporter {

    /** The data's name in the import report, such as {@code textures}. */
    String dataset();

    /** Runs the import. Blocks on disk and the database, so call it off the main thread. */
    ImportResult run() throws IOException;
}
