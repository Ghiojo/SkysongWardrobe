package org.skysongdev.skysongwardrobe.storage;

/**
 * The database could not be read or written. It is unchecked so lookups keep their simple
 * signatures, but it is never swallowed: guessing an answer when the database is down (say,
 * "this skin is not ours") would do the wrong thing, so the task that asked fails and is logged.
 */
public final class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
