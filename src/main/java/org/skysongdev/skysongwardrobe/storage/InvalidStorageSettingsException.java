package org.skysongdev.skysongwardrobe.storage;

/**
 * A {@code storage:} value the plugin can't start with. Guessing a default here could write
 * data to the wrong place and split it in two, so the plugin disables itself instead.
 */
public final class InvalidStorageSettingsException extends Exception {
    public InvalidStorageSettingsException(String message) {
        super(message);
    }
}
