package org.skysongdev.skysongwardrobe.render;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpImageFetcherTest {
    private HttpServer server;
    private final HttpImageFetcher fetcher = new HttpImageFetcher(Duration.ofSeconds(5));

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", png);
        serve("/skin", 200, png.toByteArray());
        serve("/text", 200, "hello".getBytes());
        serve("/gone", 404, new byte[0]);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void fetchesAnImage() {
        assertEquals(32, fetcher.fetch(uri("/skin")).orElseThrow().getHeight());
    }

    @Test
    void missingOrNonImagesAreEmpty() {
        assertTrue(fetcher.fetch(uri("/gone")).isEmpty());
        assertTrue(fetcher.fetch(uri("/text")).isEmpty());
    }

    private void serve(String path, int status, byte[] body) {
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }
}
