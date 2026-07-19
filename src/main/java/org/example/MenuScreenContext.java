package org.example;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.List;

/**
 * Keeps the monitor selected at the moment the menu session was opened.
 * The selection is preserved while switching between STANDARD_MENU and PROFILE,
 * and is cleared only when the overlay is fully closed.
 */
public final class MenuScreenContext {
    private Screen activeScreen;

    public Screen captureIfAbsent(double screenX, double screenY) {
        if (activeScreen == null) {
            activeScreen = findScreen(screenX, screenY);
        }
        return activeScreen;
    }

    public Screen getActiveScreenOrPrimary() {
        return activeScreen != null ? activeScreen : Screen.getPrimary();
    }

    public Rectangle2D getBoundsOrPrimary() {
        return getActiveScreenOrPrimary().getBounds();
    }

    public boolean hasActiveScreen() {
        return activeScreen != null;
    }

    public void reset() {
        activeScreen = null;
    }

    private Screen findScreen(double screenX, double screenY) {
        if (Double.isFinite(screenX) && Double.isFinite(screenY)) {
            List<Screen> matches = Screen.getScreensForRectangle(screenX, screenY, 1, 1);
            if (!matches.isEmpty()) {
                return matches.get(0);
            }
        }
        return Screen.getPrimary();
    }
}
