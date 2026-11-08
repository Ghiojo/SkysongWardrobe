package org.skysongdev.skysongwardrobe.storage.importing;

import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;
import org.skysongdev.skysongwardrobe.render.YamlTextureCache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** Copies texture-cache.yml into the database's texture cache. */
public final class TextureImporter implements DatasetImporter {
    private final Path file;
    private final TextureCache target;

    public TextureImporter(Path file, TextureCache target) {
        this.file = file;
        this.target = target;
    }

    @Override
    public String dataset() {
        return "textures";
    }

    @Override
    public ImportResult run() throws IOException {
        Map<String, SignedTexture> saved = YamlTextureCache.load(file).entries();
        int imported = target.putAllIfAbsent(saved);
        return new ImportResult(dataset(), imported, saved.size() - imported);
    }
}
