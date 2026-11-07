package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalleryFolderListenerTest {
    private ServerMock server;
    private SkysongWardrobe plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joiningRenamesTheFolderToTheCurrentName() throws IOException {
        PlayerMock player = new PlayerMock(server, "NewName");
        Path skins = plugin.getDataFolder().toPath().resolve("skins");
        Path old = Files.createDirectories(skins.resolve("OldName"));
        Files.writeString(old.resolve("gallery.yml"), "uuid: " + player.getUniqueId() + "\n");
        plugin.galleryFolders().scan();

        server.addPlayer(player);
        server.getScheduler().waitAsyncTasksFinished();

        assertEquals(Optional.of(skins.resolve("NewName")), plugin.galleryFolders().existing(player.getUniqueId()));
    }
}
