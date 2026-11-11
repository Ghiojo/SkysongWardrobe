package org.skysongdev.skysongwardrobe.render;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Hands each finished job to the listener for its wearer kind; a kind with none is ignored. */
public final class RenderRouter implements RenderListener {
    private final Map<WearerKind, RenderListener> listeners = new EnumMap<>(WearerKind.class);
    private final List<RenderListener> observers = new CopyOnWriteArrayList<>();

    /** Sends the kind's results to {@code listener}, replacing any earlier one. */
    public RenderRouter route(WearerKind kind, RenderListener listener) {
        listeners.put(kind, listener);
        return this;
    }

    /** Also sends every kind's results to {@code observer}, after the kind's own listener. */
    public RenderRouter observe(RenderListener observer) {
        observers.add(observer);
        return this;
    }

    @Override
    public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
        RenderListener listener = listeners.get(kind);
        if (listener != null) {
            listener.signed(wearer, kind, texture);
        }
        observers.forEach(observer -> observer.signed(wearer, kind, texture));
    }

    @Override
    public void failed(UUID wearer, WearerKind kind, String reason) {
        RenderListener listener = listeners.get(kind);
        if (listener != null) {
            listener.failed(wearer, kind, reason);
        }
        observers.forEach(observer -> observer.failed(wearer, kind, reason));
    }
}
