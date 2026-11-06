package org.skysongdev.skysongwardrobe.download;

/** Where uploads get their image from. Blocks, so call it off the main thread. */
@FunctionalInterface
public interface SkinSource {
    DownloadResult download(String link);
}
