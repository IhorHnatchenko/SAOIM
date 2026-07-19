package org.example;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import java.util.Objects;

/**
 * Recognizes the physical top-left-corner swipe. Permission and navigation
 * decisions are delegated to GestureRouter.
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

        if (startedInActivationZone) {
            System.out.println(
                    "[Gesture] Нажатие в левом верхнем углу монитора. Ожидаем свайп вниз..."
            );
        }
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent event) {
        if (!startedInActivationZone || gestureRecognized) {
            return;
        }

        int dragDistance = event.getY() - startY;
        if (dragDistance >= MIN_DRAG_DISTANCE) {
            gestureRecognized = true;
            System.out.println("[Gesture] Свайп вниз распознан.");
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent event) {
        if (event.getButton() != NativeMouseEvent.BUTTON1) {
            return;
        }

        try {
            if (startedInActivationZone && gestureRecognized) {
                // The monitor is selected from the activation point, not from
                // the release point after the cursor has moved down.
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
