package org.skysongdev.skysongwardrobe.download;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SkinDownloaderTest {
    private static final int MAX_BYTES = 4096;
    private final byte[] skin = PngFixtures.png(64, 64);
    private HttpServer server;
    private SkinDownloader downloader;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        serve("/skin.png", 200, skin);
        serve("/big.png", 200, new byte[MAX_BYTES + 10]);
        serve("/small.png", 200, PngFixtures.png(64, 32));
        serve("/page.html", 200, "<html></html>".getBytes());
        serve("/missing.png", 404, new byte[0]);
        server.createContext("/moved.png", exchange -> {
            exchange.getResponseHeaders().add("Location", "/skin.png");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        downloader = new SkinDownloader(Duration.ofSeconds(5), MAX_BYTES);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsASkin() {
        DownloadResult.Success result = assertInstanceOf(DownloadResult.Success.class, downloader.download(url("/skin.png")));

        assertArrayEquals(skin, result.png());
    }

    @Test
    void followsRedirects() {
        assertInstanceOf(DownloadResult.Success.class, downloader.download(url("/moved.png")));
    }

    @Test
    void eachProblemHasItsOwnReason() {
        assertFailure(DownloadFailure.TOO_LARGE, url("/big.png"));
        assertFailure(DownloadFailure.WRONG_SIZE, url("/small.png"));
        assertFailure(DownloadFailure.NOT_PNG, url("/page.html"));
        assertFailure(DownloadFailure.UNREACHABLE, url("/missing.png"));
    }

    @Test
    void closedPortIsUnreachable() {
        String link = url("/skin.png");
        server.stop(0);

        assertFailure(DownloadFailure.UNREACHABLE, link);
    }

    @ParameterizedTest
    @ValueSource(strings = {"skin.png", "file:///etc/passwd", "ftp://example.com/skin.png", "https://", "not a link"})
    void refusesLinksThatAreNotHttp(String link) {
        assertFailure(DownloadFailure.INVALID_LINK, link);
    }

    private void assertFailure(DownloadFailure reason, String link) {
        assertEquals(new DownloadResult.Failure(reason), downloader.download(link));
    }

    private void serve(String path, int status, byte[] body) {
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }
}
