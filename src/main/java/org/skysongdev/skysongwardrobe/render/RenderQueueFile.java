package org.skysongdev.skysongwardrobe.render;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** render-queue.yml: the jobs still waiting for MineSkin, so a restart never loses one. */
final class RenderQueueFile {
    private static final String JOBS = "jobs";

    private final Path file;
    private final Logger logger;

    RenderQueueFile(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /** The saved jobs in queue order. Entries that can't be read are logged and left out. */
    List<RenderJob> load() {
        List<RenderJob> jobs = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return jobs;
        }
        ConfigurationSection saved = YamlConfiguration.loadConfiguration(file.toFile()).getConfigurationSection(JOBS);
        if (saved == null) {
            return jobs;
        }
        for (String key : saved.getKeys(false)) {
            try {
                jobs.add(read(UUID.fromString(key), saved.getConfigurationSection(key)));
            } catch (IOException | RuntimeException unreadable) {
                logger.warning("Skipping render job " + key + " in " + file.getFileName() + ": " + unreadable.getMessage());
            }
        }
        return jobs;
    }

    void save(Collection<RenderJob> jobs) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (RenderJob job : jobs) {
            String path = JOBS + "." + job.wearer();
            yaml.set(path + ".kind", job.kind().name());
            yaml.set(path + ".arm-model", job.armModel().name());
            yaml.set(path + ".hash", job.hash());
            yaml.set(path + ".skin", png(job.skin()));
        }
        Files.createDirectories(file.getParent());
        yaml.save(file.toFile());
    }

    private static RenderJob read(UUID wearer, ConfigurationSection entry) throws IOException {
        if (entry == null) {
            throw new IOException("not a section");
        }
        BufferedImage skin = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(entry.getString("skin", ""))));
        if (skin == null) {
            throw new IOException("the skin is not an image");
        }
        // Jobs saved before mannequins existed have no kind and are all players.
        WearerKind kind = WearerKind.valueOf(entry.getString("kind", WearerKind.PLAYER.name()));
        return new RenderJob(wearer, kind, skin, ArmModel.valueOf(entry.getString("arm-model", "")), entry.getString("hash", ""));
    }

    private static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
