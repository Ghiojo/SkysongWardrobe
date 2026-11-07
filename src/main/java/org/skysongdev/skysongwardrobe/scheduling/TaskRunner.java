package org.skysongdev.skysongwardrobe.scheduling;

/** Runs work off or on the main thread. Lets the flows that download or read files be tested without a scheduler. */
public interface TaskRunner {
    /** Runs the task off the main thread. */
    void async(Runnable task);

    /** Runs the task on the main thread, on the next tick. */
    void sync(Runnable task);
}
