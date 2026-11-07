package org.skysongdev.skysongwardrobe.items;

import org.skysongdev.skysongwardrobe.scheduling.TrackingTaskRunner;

import java.util.function.Supplier;

/** Hands out item tags like SkysongItems does: refused off the main thread, counted on it. */
public final class MainThreadItemTags implements Supplier<ItemTagGateway> {
    private final TrackingTaskRunner tasks;
    private final ItemTagGateway tags;
    private int reads;

    public MainThreadItemTags(TrackingTaskRunner tasks, ItemTagGateway tags) {
        this.tasks = tasks;
        this.tags = tags;
    }

    @Override
    public ItemTagGateway get() {
        if (tasks.offMainThread()) {
            throw new IllegalStateException("SkysongItems API item reads and writes must run on the server's main thread");
        }
        reads++;
        return tags;
    }

    /** How many times the tags were taken on the main thread. */
    public int reads() {
        return reads;
    }
}
