package org.skysongdev.skysongwardrobe.render;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Hands each finished job to the listener for its wearer kind; a kind with none is ignored. */
public final class RenderRouter implements RenderListener {
    private final Map<WearerKind, RenderListener> listeners = new EnumMap<>(WearerKind.class);

    /** Sends the kind's results to {@code listener}, replacing any earlier one. */
    public RenderRouter route(WearerKind kind, RenderListener listener) {
        listeners.put(kind, listener);
        return this;
    }

    @Override
    public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
        RenderListener listener = listeners.get(kind);
        if (listener != null) {
            listener.signed(wearer, kind, texture);
        }
    }

    @Override
    public void failed(UUID wearer, WearerKind kind, String reason) {
        RenderListener listener = listeners.get(kind);
        if (listener != null) {
            listener.failed(wearer, kind, reason);
        }
    }
}
