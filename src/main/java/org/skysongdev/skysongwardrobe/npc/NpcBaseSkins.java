package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.render.BaseSkin;
import org.skysongdev.skysongwardrobe.render.BaseSkins;
import org.skysongdev.skysongwardrobe.render.SignedTexture;
import org.skysongdev.skysongwardrobe.render.TextureCache;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The skin each NPC has under its outfit. Like a player's, the NPC's current Citizens skin
 * becomes the base unless this plugin rendered it, so {@code /npc skin} is always built on.
 * Unlike a player's, the base is saved in the NPC's record: after a restart a dressed NPC only
 * holds the rendered skin, and its original would otherwise be lost.
 */
public final class NpcBaseSkins {
    private final NpcRepository records;
    private final TextureCache cache;
    private final BaseSkins images;
    private final Logger logger;

    /**
     * @param images downloads and remembers base images, keyed by NPC id; only ever handed
     *               textures this plugin did not render
     */
    public NpcBaseSkins(NpcRepository records, TextureCache cache, BaseSkins images, Logger logger) {
        this.records = records;
        this.cache = cache;
        this.images = images;
        this.logger = logger;
    }

    /**
     * The NPC's base skin, taking {@code current} (its Citizens skin right now) as the new base
     * when it isn't one of ours and saving it. Downloads the image, so call it off the main
     * thread. Empty when the NPC has no skin of its own yet, or it can't be downloaded.
     */
    public Optional<BaseSkin> resolve(UUID npc, Optional<SignedTexture> current) {
        Optional<SignedTexture> stored = records.find(npc).map(NpcRecord::base);
        Optional<SignedTexture> base = current.filter(texture -> !cache.isOwn(texture)).or(() -> stored);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        if (!base.equals(stored)) {
            save(new NpcRecord(npc, base.get()));
        }
        return images.resolve(npc, base);
    }

    /** The saved base texture, the skin the NPC goes back to when it wears nothing. */
    public Optional<SignedTexture> saved(UUID npc) {
        return records.find(npc).map(NpcRecord::base);
    }

    private void save(NpcRecord record) {
        try {
            records.save(record);
        } catch (IOException failed) {
            logger.log(Level.WARNING, "Could not save the base skin of NPC " + record.id()
                    + "; it is used now but may be lost on restart.", failed);
        }
    }
}
