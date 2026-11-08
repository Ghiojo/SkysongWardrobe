package org.skysongdev.skysongwardrobe.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncWriterTest {

    @Test
    void runsTasksInOrderOnItsOwnThread() {
        List<Integer> ran = new CopyOnWriteArrayList<>();
        Thread caller = Thread.currentThread();
        try (AsyncWriter writer = AsyncWriter.background()) {
            for (int i = 0; i < 100; i++) {
                int number = i;
                writer.submit(() -> {
                    assertNotEquals(caller, Thread.currentThread());
                    ran.add(number);
                    return null;
                });
            }
            writer.flush().join();
        }

        assertEquals(IntStream.range(0, 100).boxed().toList(), ran);
    }

    @Test
    void aFailedTaskFailsItsFutureAndTheNextStillRuns() {
        try (AsyncWriter writer = AsyncWriter.background()) {
            CompletableFuture<Object> failed = writer.submit(() -> {
                throw new IOException("disk on fire");
            });
            CompletableFuture<String> next = writer.submit(() -> "fine");

            CompletionException thrown = assertThrows(CompletionException.class, failed::join);
            assertInstanceOf(IOException.class, thrown.getCause());
            assertEquals("fine", next.join());
        }
    }

    @Test
    void closeWaitsForQueuedTasks() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AsyncWriter writer = AsyncWriter.background();
        writer.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                throw new IOException(interrupted);
            }
            done.countDown();
            return null;
        });

        writer.close();

        assertTrue(done.await(0, TimeUnit.MILLISECONDS));
    }

    @Test
    void directRunsOnTheCallersThread() {
        Thread caller = Thread.currentThread();
        AsyncWriter writer = AsyncWriter.direct();

        CompletableFuture<Thread> ran = writer.submit(Thread::currentThread);

        assertTrue(ran.isDone());
        assertEquals(caller, ran.join());
        assertThrows(CompletionException.class, () -> writer.submit(() -> {
            throw new IOException("x");
        }).join());
    }
}
