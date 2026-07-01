package org.example;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

public class MouseHookHandler implements NativeMouseInputListener {

    private final int ZONE_SIZE = 20;
    private final int MIN_DRAG_DISTANCE = 100;

    private int startX;
    private int startY;
    private boolean isStartedInZone = false;
    private boolean gestureRecognized = false;

    @Override
    public void nativeMousePressed(NativeMouseEvent event) {
        if (event.getButton() == NativeMouseEvent.BUTTON1) {
            startX = event.getX();
            startY = event.getY();

            if (startX <= ZONE_SIZE && startY <= ZONE_SIZE) {
                isStartedInZone = true;
                gestureRecognized = false;
                System.out.println("[System] Клик в зоне запуска! Ожидание движения вниз...");
            } else {
                isStartedInZone = false;
            }
        }
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent event) {
        if (isStartedInZone) {
            int currentY = event.getY();
            int dragDistance = currentY - startY;

            if (dragDistance >= MIN_DRAG_DISTANCE && !gestureRecognized) {
                gestureRecognized = true;
                System.out.println("[System] ЖЕСТ РАСПОЗНАН: Движение вниз выполнено.");
            }
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent event) {
        if (event.getButton() == NativeMouseEvent.BUTTON1) {
            if (isStartedInZone && gestureRecognized) {

                if (WindowsUtils.isDesktopActive()) {
                    System.out.println("[System] Действие: Открываем SAO Menu на рабочем столе.");
                    // ТЕПЕРЬ ОШИБКИ НЕТ! Метод успешно вызовется
                    MainStarter.triggerMenu(event.getX(), event.getY());
                } else {
                    System.out.println("[System] Отмена: мы не на рабочем столе.");
                }
            }

            isStartedInZone = false;
            gestureRecognized = false;
        }
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent event) {}
    @Override
    public void nativeMouseMoved(NativeMouseEvent event) {}
}