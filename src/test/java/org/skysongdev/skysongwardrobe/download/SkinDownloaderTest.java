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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinDownloaderTest {
    private static final int MAX_BYTES = 4096;
    private final byte[] skin = PngFixtures.png(64, 64);
    private static final String BLOCKED_HOST = "localhost";
    private final List<String> checkedHosts = new ArrayList<>();
    private final List<String> requests = new ArrayList<>();
    private final List<String> userAgents = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
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
        redirect("/moved.png", "/skin.png");
        redirect("/to-blocked.png", "http://" + BLOCKED_HOST + ":1/skin.png");
        redirect("/to-ftp.png", "ftp://example.com/skin.png");
        redirect("/loop.png", "/loop.png");
        server.start();
        // The test server is on loopback, so only BLOCKED_HOST stands in for a private address.
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        downloader = new SkinDownloader(Duration.ofSeconds(5), MAX_BYTES, host -> {
            checkedHosts.add(host);
            return BLOCKED_HOST.equals(host) ? Optional.of(DownloadFailure.PRIVATE_ADDRESS) : Optional.empty();
        }, logger);
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
    void everyRedirectHostIsChecked() {
        downloader.download(url("/moved.png"));

        assertEquals(List.of("127.0.0.1", "127.0.0.1"), checkedHosts);
    }

    @Test
    void refusedHostIsNeverContacted() {
        assertFailure(DownloadFailure.PRIVATE_ADDRESS, "http://" + BLOCKED_HOST + ":" + server.getAddress().getPort() + "/skin.png");

        assertTrue(requests.isEmpty());
    }

    @Test
    void redirectToARefusedHostIsRefused() {
        assertFailure(DownloadFailure.PRIVATE_ADDRESS, url("/to-blocked.png"));
    }

    @Test
    void redirectThatIsNotHttpOrLoopsForeverIsUnreachable() {
        assertFailure(DownloadFailure.UNREACHABLE, url("/to-ftp.png"));
        assertFailure(DownloadFailure.UNREACHABLE, url("/loop.png"));
    }

    @Test
    void requestsNameThePluginInsteadOfJava() {
        downloader.download(url("/skin.png"));

        assertEquals(List.of(SkinDownloader.USER_AGENT), userAgents);
    }

    @Test
    void anUnreachableLinkIsLoggedWithTheStatus() {
        downloader.download(url("/missing.png"));

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().endsWith("/missing.png: HTTP 404"), warnings.getFirst());
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

    private void redirect(String path, String location) {
        server.createContext(path, exchange -> {
            requests.add(path);
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
    }

    private void serve(String path, int status, byte[] body) {
        server.createContext(path, exchange -> {
            requests.add(path);
            userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
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
