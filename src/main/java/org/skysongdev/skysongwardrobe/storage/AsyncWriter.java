package org.skysongdev.skysongwardrobe.storage;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs storage work on one background thread, one task after another in the order it was given,
 * as SkysongStats does: the main thread never waits on a file or a database, and a read queued
 * after a write always sees it. {@link #direct()} runs everything on the caller's thread instead,
 * for tests and code that must finish before going on.
 */
public final class AsyncWriter implements AutoCloseable {
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    /** Storage work that may fail. */
    @FunctionalInterface
    public interface Task<T> {
        T run() throws IOException;
    }

    private final ExecutorService executor;

    private AsyncWriter(ExecutorService executor) {
        this.executor = executor;
    }

    /** A writer with its own background thread. */
    public static AsyncWriter background() {
        return new AsyncWriter(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkysongWardrobe storage writer");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** A writer that runs each task at once on the caller's thread. */
    public static AsyncWriter direct() {
        return new AsyncWriter(null);
    }

    /** Queues the task; the future completes with its result, or with the exception it threw. */
    public <T> CompletableFuture<T> submit(Task<T> task) {
        if (executor == null) {
            return run(task);
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.execute(() -> run(task).whenComplete((value, failed) -> {
            if (failed != null) {
                result.completeExceptionally(failed);
            } else {
                result.complete(value);
            }
        }));
        return result;
    }

    /** Completes once every task queued before it has run, for tests and shutdown. */
    public CompletableFuture<Void> flush() {
        return submit(() -> null);
    }

    private static <T> CompletableFuture<T> run(Task<T> task) {
        try {
            return CompletableFuture.completedFuture(task.run());
        } catch (IOException | RuntimeException failed) {
            return CompletableFuture.failedFuture(failed);
        }
    }

    /** Stops taking tasks and waits up to 30 seconds for the queued ones, for plugin shutdown. */
    @Override
    public void close() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            executor.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
