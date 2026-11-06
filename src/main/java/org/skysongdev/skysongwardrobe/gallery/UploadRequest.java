package org.skysongdev.skysongwardrobe.gallery;

import org.bukkit.Material;

import java.net.URI;
import java.util.Optional;

/** What {@code /sw upload} was asked to save, already checked for format. */
public record UploadRequest(SkinId id, URI link, Optional<Material> icon, String name) {
}
