package org.skysongdev.skysongwardrobe.download;

/** The outcome of downloading a skin: the PNG's bytes, or why there are none. */
public sealed interface DownloadResult {
    record Success(byte[] png) implements DownloadResult {
    }

    record Failure(DownloadFailure reason) implements DownloadResult {
    }

    static DownloadResult failed(DownloadFailure reason) {
        return new Failure(reason);
    }
}
