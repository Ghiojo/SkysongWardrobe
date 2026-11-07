package org.skysongdev.skysongwardrobe.prompt;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The two answers to one prompt, of which only the first click counts. A player could
 * otherwise click Agree twice, or Agree then Decline, on a dialog the client still shows.
 */
final class PromptAnswer {
    private final AtomicBoolean answered = new AtomicBoolean();
    private final Runnable onAgree;
    private final Runnable onDecline;

    PromptAnswer(Runnable onAgree, Runnable onDecline) {
        this.onAgree = onAgree;
        this.onDecline = onDecline;
    }

    void agree() {
        if (answered.compareAndSet(false, true)) {
            onAgree.run();
        }
    }

    void decline() {
        if (answered.compareAndSet(false, true)) {
            onDecline.run();
        }
    }
}
