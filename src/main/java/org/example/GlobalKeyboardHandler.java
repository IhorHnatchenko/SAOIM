package org.example;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides a reliable Escape fallback even when a transparent part of the
 * overlay loses JavaFX keyboard focus to the Windows desktop.
 */
public final class GlobalKeyboardHandler implements NativeKeyListener {

    private final Runnable escapeAction;
    private final AtomicBoolean escapePressed = new AtomicBoolean(false);

    public GlobalKeyboardHandler(Runnable escapeAction) {
        this.escapeAction = Objects.requireNonNull(escapeAction, "escapeAction");
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent event) {
        if (event.getKeyCode() != NativeKeyEvent.VC_ESCAPE) {
            return;
        }

        // Native key-repeat must not close two levels during one key press.
        if (escapePressed.compareAndSet(false, true)) {
            escapeAction.run();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent event) {
        if (event.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            escapePressed.set(false);
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent event) {
        // Not used.
    }
}
