package org.skysongdev.skysongwardrobe.download;

/** Why a skin link could not be used. Each has its own text in messages.yml. */
public enum DownloadFailure {
    /** Not an http or https link. */
    INVALID_LINK,
    /** The link, or a redirect, points to the server itself or its private network. */
    PRIVATE_ADDRESS,
    /** No answer, an error status, or the timeout ran out. */
    UNREACHABLE,
    /** Larger than {@code gallery.max-download-kb}. */
    TOO_LARGE,
    /** Not a PNG image. */
    NOT_PNG,
    /** A PNG, but not 64x64. */
    WRONG_SIZE
}
