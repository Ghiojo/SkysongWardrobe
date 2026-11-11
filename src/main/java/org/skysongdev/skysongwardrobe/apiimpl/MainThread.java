package org.skysongdev.skysongwardrobe.apiimpl;

import java.util.function.BooleanSupplier;

// Guards the API methods: menus, wardrobes and Citizens are main-thread only, so a call from
// another thread is refused instead of racing them.
final class MainThread {
    private final BooleanSupplier isMainThread;

    MainThread(BooleanSupplier isMainThread) {
        this.isMainThread = isMainThread;
    }

    void check() {
        if (!isMainThread.getAsBoolean()) {
            throw new IllegalStateException("The SkysongWardrobe API must be called on the server's main thread."
                    + " Use Bukkit.getScheduler().runTask(...) to get back to it.");
        }
    }
}
