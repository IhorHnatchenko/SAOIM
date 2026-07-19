package org.example;

/**
 * Determines whether a hidden SAOIM overlay may be opened by the global
 * gesture. The hidden overlay is allowed to open only when Windows Explorer's
 * desktop is the foreground context.
 */
public final class DesktopContextDetector {

    public boolean isDesktopActive() {
        try {
            return WindowsUtils.isDesktopActive();
        } catch (RuntimeException exception) {
            System.err.println(
                    "[Gesture] Не удалось определить активный контекст Windows: "
                            + exception.getMessage()
            );
            return false;
        }
    }
}
