package org.skysongdev.skysongwardrobe.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/** {@link ImageFetcher} over HTTP, for base skins on textures.minecraft.net. */
public final class HttpImageFetcher implements ImageFetcher {
    private static final int OK = 200;
    /** Skin textures are a few kilobytes; anything far larger is not one. */
    private static final int MAX_BYTES = 256 * 1024;

    private final HttpClient client;
    private final Duration timeout;

    public HttpImageFetcher(Duration timeout) {
        this.client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
        this.timeout = timeout;
    }

    @Override
    public Optional<BufferedImage> fetch(URI uri) {
        try {
            HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != OK) {
                    return Optional.empty();
                }
                byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                return bytes.length > MAX_BYTES ? Optional.empty() : Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(bytes)));
            }
        } catch (IOException | IllegalArgumentException failed) {
            return Optional.empty();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
