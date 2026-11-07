package org.skysongdev.skysongwardrobe.scheduling;

/** Runs every task at once, on the calling thread. */
public final class ImmediateTaskRunner implements TaskRunner {
    @Override
    public void async(Runnable task) {
        task.run();
    }

    @Override
    public void sync(Runnable task) {
        task.run();
    }
}
