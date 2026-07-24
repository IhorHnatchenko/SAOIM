package org.example;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import java.util.Objects;

/**
 * Recognizes only the physical top-left-corner swipe. Secondary-button input
 * is intentionally handled by the JavaFX overlay so the complete press/release
 * sequence stays inside one window and Windows Explorer cannot open its own
 * context menu.
 */
public final class MouseHookHandler implements NativeMouseInputListener {

    private static final int ZONE_SIZE = 20;
    private static final int MIN_DRAG_DISTANCE = 100;

    private final GestureRouter gestureRouter;
    private final ScreenService screenService;

    private int startX;
    private int startY;
    private boolean startedInActivationZone;
    private boolean gestureRecognized;

    public MouseHookHandler(GestureRouter gestureRouter, ScreenService screenService) {
        this.gestureRouter = Objects.requireNonNull(gestureRouter, "gestureRouter");
        this.screenService = Objects.requireNonNull(screenService, "screenService");
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent event) {
        if (event.getButton() != NativeMouseEvent.BUTTON1) {
            return;
        }

        startX = event.getX();
        startY = event.getY();
        gestureRecognized = false;
        startedInActivationZone = screenService.isInTopLeftActivationZone(
                startX,
                startY,
                ZONE_SIZE
        );
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent event) {
        if (!startedInActivationZone || gestureRecognized) {
            return;
        }

        int dragDistance = event.getY() - startY;
        if (dragDistance >= MIN_DRAG_DISTANCE) {
            gestureRecognized = true;
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent event) {
        if (event.getButton() != NativeMouseEvent.BUTTON1) {
            return;
        }

        try {
            if (startedInActivationZone && gestureRecognized) {
                gestureRouter.handleOpenGesture(startX, startY);
            }
        } finally {
            resetGesture();
        }
    }

    private void resetGesture() {
        startedInActivationZone = false;
        gestureRecognized = false;
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent event) {
        // Not used.
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent event) {
        // Not used.
    }
}
