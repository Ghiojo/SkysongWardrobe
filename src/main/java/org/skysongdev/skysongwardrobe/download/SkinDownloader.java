package org.skysongdev.skysongwardrobe.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Downloads a skin image from a direct link and checks it. Blocks for up to the timeout,
 * so call it off the main thread. Reads at most {@code maxBytes} + 1 bytes, whatever the
 * server claims, so a huge file can't fill memory. Redirects are followed by hand so the
 * address policy checks every host on the way, not only the first.
 */
public final class SkinDownloader implements SkinSource {
    private static final Set<String> SCHEMES = Set.of("http", "https");
    private static final int OK_MIN = 200;
    private static final int OK_MAX = 299;
    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);
    private static final int MAX_REDIRECTS = 5;

    private final HttpClient client;
    private final Duration timeout;
    private final int maxBytes;
    private final AddressPolicy addresses;

    public SkinDownloader(Duration timeout, int maxBytes, AddressPolicy addresses) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.timeout = timeout;
        this.maxBytes = maxBytes;
        this.addresses = addresses;
    }

    /** The link as a URI if it is an http or https link with a host, which is all this downloads. */
    public static Optional<URI> httpLink(String link) {
        try {
            URI uri = URI.create(link.trim());
            boolean http = uri.getScheme() != null && SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT));
            return http && uri.getHost() != null ? Optional.of(uri) : Optional.empty();
        } catch (IllegalArgumentException notALink) {
            return Optional.empty();
        }
    }

    @Override
    public DownloadResult download(String link) {
        Optional<URI> checked = httpLink(link);
        if (checked.isEmpty()) {
            return DownloadResult.failed(DownloadFailure.INVALID_LINK);
        }
        try {
            return fetch(checked.get());
        } catch (IOException | IllegalArgumentException failed) {
            return DownloadResult.failed(DownloadFailure.UNREACHABLE);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return DownloadResult.failed(DownloadFailure.UNREACHABLE);
        }
    }

    private DownloadResult fetch(URI first) throws IOException, InterruptedException {
        URI uri = first;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Optional<DownloadFailure> refused = addresses.check(uri.getHost());
            if (refused.isPresent()) {
                return DownloadResult.failed(refused.get());
            }
            HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (REDIRECTS.contains(status)) {
                    URI current = uri;
                    Optional<URI> next = response.headers().firstValue("Location")
                            .flatMap(location -> httpLink(current.resolve(location.trim()).toString()));
                    if (next.isEmpty()) {
                        return DownloadResult.failed(DownloadFailure.UNREACHABLE);
                    }
                    uri = next.get();
                    continue;
                }
                if (status < OK_MIN || status > OK_MAX) {
                    return DownloadResult.failed(DownloadFailure.UNREACHABLE);
                }
                return checkImage(body.readNBytes(maxBytes + 1));
            }
        }
        return DownloadResult.failed(DownloadFailure.UNREACHABLE);
    }

    private DownloadResult checkImage(byte[] bytes) {
        if (bytes.length > maxBytes) {
            return DownloadResult.failed(DownloadFailure.TOO_LARGE);
        }
        return SkinImageCheck.check(bytes)
                .<DownloadResult>map(DownloadResult.Failure::new)
                .orElseGet(() -> new DownloadResult.Success(bytes));
    }
}
