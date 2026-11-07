package org.skysongdev.skysongwardrobe.commands;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.gallery.PlayerGallery;
import org.skysongdev.skysongwardrobe.gallery.SkinEntry;
import org.skysongdev.skysongwardrobe.gallery.SkinId;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/** Shared setup for the gallery command tests. */
final class GalleryCommandFixture {
    private GalleryCommandFixture() {
    }

    static PlayerGallery galleryWith(SkysongWardrobe plugin, PlayerMock player, String... ids) throws IOException {
        PlayerGallery gallery = plugin.galleries().claim(player.getUniqueId(), player.getName());
        for (String id : ids) {
            gallery.add(new SkinEntry(new SkinId(id), id, Optional.empty(),
                    URI.create("https://example.com/" + id + ".png"), Instant.EPOCH), new byte[] {1});
        }
        return gallery;
    }

    static String nextMessage(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
    }
}
