package org.skysongdev.skysongwardrobe.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Downloads a skin image from a direct link and checks it. Blocks for up to the timeout,
 * so call it off the main thread. Reads at most {@code maxBytes} + 1 bytes, whatever the
 * server claims, so a huge file can't fill memory.
 */
public final class SkinDownloader {
    private static final Set<String> SCHEMES = Set.of("http", "https");
    private static final int OK_MIN = 200;
    private static final int OK_MAX = 299;

    private final HttpClient client;
    private final Duration timeout;
    private final int maxBytes;

    public SkinDownloader(Duration timeout, int maxBytes) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.timeout = timeout;
        this.maxBytes = maxBytes;
    }

    public DownloadResult download(String link) {
        URI uri;
        try {
            uri = URI.create(link.trim());
        } catch (IllegalArgumentException notALink) {
            return DownloadResult.failed(DownloadFailure.INVALID_LINK);
        }
        if (uri.getScheme() == null || !SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT)) || uri.getHost() == null) {
            return DownloadResult.failed(DownloadFailure.INVALID_LINK);
        }
        byte[] bytes;
        try {
            HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < OK_MIN || response.statusCode() > OK_MAX) {
                    return DownloadResult.failed(DownloadFailure.UNREACHABLE);
                }
                bytes = body.readNBytes(maxBytes + 1);
            }
        } catch (IOException failed) {
            return DownloadResult.failed(DownloadFailure.UNREACHABLE);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return DownloadResult.failed(DownloadFailure.UNREACHABLE);
        }
        if (bytes.length > maxBytes) {
            return DownloadResult.failed(DownloadFailure.TOO_LARGE);
        }
        return SkinImageCheck.check(bytes)
                .<DownloadResult>map(DownloadResult.Failure::new)
                .orElseGet(() -> new DownloadResult.Success(bytes));
    }
}
