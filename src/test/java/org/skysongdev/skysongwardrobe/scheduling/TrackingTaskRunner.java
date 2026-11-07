package org.skysongdev.skysongwardrobe.scheduling;

/**
 * Runs every task at once, like {@link ImmediateTaskRunner}, but remembers whether an async task
 * is running, so tests can fail work that must stay on the main thread.
 */
public final class TrackingTaskRunner implements TaskRunner {
    private boolean offMainThread;

    @Override
    public void async(Runnable task) {
        boolean outer = offMainThread;
        offMainThread = true;
        try {
            task.run();
        } finally {
            offMainThread = outer;
        }
    }

    @Override
    public void sync(Runnable task) {
        boolean outer = offMainThread;
        offMainThread = false;
        try {
            task.run();
        } finally {
            offMainThread = outer;
        }
    }

    /** Whether the calling code is inside an async task. */
    public boolean offMainThread() {
        return offMainThread;
    }
}
