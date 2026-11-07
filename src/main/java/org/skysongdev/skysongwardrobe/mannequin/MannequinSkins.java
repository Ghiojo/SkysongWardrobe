package org.skysongdev.skysongwardrobe.mannequin;

import org.skysongdev.skysongwardrobe.download.SkinImageCheck;
import org.skysongdev.skysongwardrobe.gallery.SkinId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The preset base skins mannequins wear under their outfit: every 64x64 PNG in the
 * mannequin-skins folder, named by its file, so {@code stone.png} is the skin {@code stone}.
 * A file that isn't a usable skin, or whose name isn't a valid id, is skipped with a warning.
 * A loaded set never changes; a reload builds a new one.
 */
public final class MannequinSkins {
    private static final String EXTENSION = ".png";

    private final Map<String, BufferedImage> skins;

    private MannequinSkins(Map<String, BufferedImage> skins) {
        this.skins = skins;
    }

    /** Reads the folder, creating it if missing. */
    public static MannequinSkins load(Path folder, Logger logger) {
        Map<String, BufferedImage> skins = new TreeMap<>();
        try {
            Files.createDirectories(folder);
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION))
                        .sorted()
                        .forEach(file -> read(file, logger).ifPresent(image -> skins.put(id(file), image)));
            }
        } catch (IOException broken) {
            logger.warning("Could not read " + folder + ", so mannequins have no skins: " + broken.getMessage());
        }
        return new MannequinSkins(skins);
    }

    /** The skin with this id, as a copy the caller may draw on; ids are matched lowercased. */
    public Optional<BufferedImage> image(String id) {
        return SkinId.parse(id)
                .map(parsed -> skins.get(parsed.value()))
                .map(MannequinSkins::copy);
    }

    /** Whether a skin with this id exists. */
    public boolean has(String id) {
        return SkinId.parse(id).map(parsed -> skins.containsKey(parsed.value())).orElse(false);
    }

    /** Every skin id. */
    public Set<String> ids() {
        return Set.copyOf(skins.keySet());
    }

    private static Optional<BufferedImage> read(Path file, Logger logger) {
        String name = id(file);
        if (!name.equals(name.toLowerCase(Locale.ROOT)) || SkinId.parse(name).isEmpty()) {
            logger.warning(file + " is skipped: mannequin skin names may only use a-z, 0-9 and _, up to "
                    + SkinId.MAX_LENGTH + " characters.");
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (SkinImageCheck.check(bytes).isPresent()) {
                logger.warning(file + " is skipped: it is not a 64x64 PNG.");
                return Optional.empty();
            }
            return Optional.of(ImageIO.read(new ByteArrayInputStream(bytes)));
        } catch (IOException broken) {
            logger.warning(file + " is skipped: " + broken.getMessage());
            return Optional.empty();
        }
    }

    private static String id(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(image, 0, 0, null);
        return copy;
    }
}
