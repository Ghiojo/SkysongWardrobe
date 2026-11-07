package org.skysongdev.skysongwardrobe.render;

import java.util.UUID;

/** Hears how each {@link RenderJob} ended. Called on the queue's thread, not the main thread. */
public interface RenderListener {
    /** The wearer's skin is signed (or was already in the cache) and can be applied. */
    void signed(UUID wearer, WearerKind kind, SignedTexture texture);

    /** The skin could not be signed; the wearer keeps the skin they have. */
    void failed(UUID wearer, WearerKind kind, String reason);
}
